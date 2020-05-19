package com.ml.mlchat

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Contacts
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.ml.mlchat.model.PeopleDTO
import com.ml.mlchat.util.PeopleFragment
import kotlinx.android.synthetic.main.activity_home.*

//로그인 이후 첫 메인 액티비티
class HomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener{
    var firestore : FirebaseFirestore? = null

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when (p0.itemId) {
            R.id.action_people -> {
                var peopleFragment = PeopleFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.home_framelayout, peopleFragment).commit()
                return true
            }
            R.id.action_chat -> {
                Log.v("채팅액티비티","on")
                return true
            }
            R.id.action_settings -> {
                Log.v("세팅액티비티","on")
                return true
            }
        }
        return false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        bottom_navigation.setOnNavigationItemSelectedListener(this)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        //상태바 흰색, 아이콘 검은색
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        registerPushToken() //토큰생성 및 데이터베이스 저장

        firestore = FirebaseFirestore.getInstance() //초기화

        var uid = FirebaseAuth.getInstance().currentUser?.uid
        var userEmail = FirebaseAuth.getInstance().currentUser?.email

        bottom_navigation.selectedItemId = R.id.action_people //기본화면설정

        Log.v("유저정보",uid)
        Log.v("유저 이메일", userEmail)

    }

    fun registerPushToken(){
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener {
                task ->
            val token = task.result?.token
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String,Any>()
            map["pushToken"] = token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)

        }
    }
}
