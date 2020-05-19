package com.ml.mlchat

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_sign_up.*
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.graphics.Color
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.Build
import android.view.View


//로그인 액티비티
class MainActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //상태바 흰색, 아이콘 검은색
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }

        mAuth = FirebaseAuth.getInstance()
        val pref = getSharedPreferences("mine", Context.MODE_PRIVATE)
        val editor = pref.edit()

        id_edittext.setText(pref.getString("ID", ""))
        /* mainEt(EditText)의 텍스트를 "MessageKey"에 해당하는 vaule로 설정.
         * 값을 불러오지 못했을 경우, default vaule는 ""로 지정. */

        email_login_btn.setOnClickListener{
            editor.putString("ID", id_edittext.text.toString()).apply()
            signinEmail()
        }

        email_signup_btn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }




    fun signinEmail(){
        mAuth?.signInWithEmailAndPassword(id_edittext.text.toString(), pw_edittext.text.toString())
            ?.addOnCompleteListener(){
                    task ->
                if(task.isSuccessful){
                    moveMainPage(task.result?.user)
                }else{
                    Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    //Firebase 유저 상태 넘겨줌
    fun moveMainPage(user: FirebaseUser?){
        if(user != null && FirebaseAuth.getInstance().currentUser!!.isEmailVerified){ //유저상태가 있으면 메인액티비티 호출
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
            Log.v("메인페이지 이동","이동")
        }else{
            Log.e("인증되지 않은 이메일", "인증안됨")
            Toast.makeText(this, "이메일 인증이 완료되지 않았습니다..", Toast.LENGTH_SHORT).show()
        }
    }

}
