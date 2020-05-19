package com.ml.mlchat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.ml.mlchat.model.PeopleDTO
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_sign_up.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class SignUpActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    var uid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid

        signup_btn.setOnClickListener {
            userNameCheck()
        }
    }

    fun userNameCheck(){
        if(signup_username_edittext.text.toString() == null || signup_username_edittext.text.toString() == ""){
            Log.v("닉네임 확인부탁","ㅇㅇ")
            Toast.makeText(this, "닉네임을 적어주세요.", Toast.LENGTH_SHORT).show()
        }else{
            signinAndSignup()
        }
    }
    private fun signinAndSignup() {
        mAuth?.createUserWithEmailAndPassword(
            signup_email_edittext.text.toString(),
            signup_pw_edittext.text.toString()
        )?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                mAuth?.currentUser?.sendEmailVerification()?.addOnCompleteListener {
                    if(it.isSuccessful){
                        Log.v("이메일 링크 전송 성공","성공")
                        Toast.makeText(this, "이메일을 확인해주세요.", Toast.LENGTH_SHORT).show()

                        var uid = FirebaseAuth.getInstance().currentUser?.uid
                        var userEmail = FirebaseAuth.getInstance().currentUser?.email

                        var peopleDTO = PeopleDTO()
                        peopleDTO.userEmail = userEmail
                        peopleDTO.timestamp = System.currentTimeMillis()
                        peopleDTO.userName = signup_username_edittext.text.toString()
                        peopleDTO.uid = uid

                        //Cloud Firestore에 데이터 저장(가벼운 데이터들을 자주 처리할때 용이)
                        if (uid != null) {
                            FirebaseFirestore.getInstance().collection("user").document(uid)
                                .set(peopleDTO)
                                .addOnSuccessListener { Log.d("회원가입 성공", "DocumentSnapshot successfully written!") }
                                .addOnFailureListener { e -> Log.w("회원가입 실패", "Error writing document", e) }
                        }
                        moveLoginPage()
                    }else{
                        Log.e("이메일 링크 전송 실패", "sendEmailVerification", task.getException())
                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (task.exception?.message.isNullOrEmpty()) { //실패시 메세지 출력
                //Show the error message
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            } else { //로그인 하는 부분

            }
        }
    }
    private fun moveLoginPage(){
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
