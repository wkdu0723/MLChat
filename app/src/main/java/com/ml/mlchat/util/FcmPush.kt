package com.ml.mlchat.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.ml.mlchat.MainActivity
import com.ml.mlchat.R
import com.ml.mlchat.model.PushDTO
import okhttp3.*
import java.io.IOException

class FcmPush : FirebaseMessagingService(){
    var JSON = MediaType.parse("application/json; charset=utf-8")
    var url = "https://fcm.googleapis.com/fcm/send"
    var serverkey = "AAAAEZ-PgLc:APA91bH6lNPspAYFjM0vvH3TRzc6bNi3IpuvqFhMbLJIKQxjgDAMwZCBSZ9rpWif54IfZgRM-zqcYpP14ffgDBACQxsSdEzuHxAVczo1hGp11viiLwkDj6c1VOulboCC2mmCCrVIQGoo"
    var gson : Gson? = null
    var okHttpClient : OkHttpClient? = null
    lateinit var wakeLock : PowerManager.WakeLock

    //remoteMessage가 푸시 알람이 도착했다는것을 알려줌
    @RequiresApi(api = Build.VERSION_CODES.P)
    @SuppressLint("InvalidWakeLockTag")
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        var powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        //PowerManger.SCREEN_BRIGHT_WAKE_LOCK : CPU와 화면을 밝게하며 키보드는 off 상태
        //PowerManger.DIM_WAKE_LOCK : 화면을 살짝 어둡게 킴
        //PowerManager.ACQUIRE_CAUSES_WAKEUP : 조명이 켜지도록함
        //PowerManager.ON_AFTER_RELEASE : 조명이 오래 유지되도록 함
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "WAKELOCK"
        )
        wakeLock.acquire() // WakeLock 깨우기
        wakeLock.release() // WakeLock 해제
        @SuppressLint("LongLogTAG")
        if(remoteMessage?.notification != null){
            sendNotification(remoteMessage.notification?.title!!, remoteMessage.notification!!.body!!)
        }else{
        }

    }
    companion object {
        var instance = FcmPush()
    }

    init{
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid : String, title : String, message : String){
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get().addOnCompleteListener{
            task ->
            if(task.isSuccessful){
                var token = task?.result?.get("pushToken").toString()
                var pushDTO = PushDTO()
                pushDTO.to = token
                pushDTO.notification.title = title
                pushDTO.notification.body = message

                var body = RequestBody.create(JSON,gson?.toJson(pushDTO))
                var request = Request.Builder()
                    .addHeader("Cpntent-Type","application/json")
                    .addHeader("Authorization","key="+serverkey)
                    .url(url)
                    .post(body)
                    .build()

                okHttpClient?.newCall(request)?.enqueue(object : Callback{
                    override fun onFailure(call: Call?, e: IOException?) {
                    }

                    override fun onResponse(call: Call?, response: Response?) {
                        println(response?.body()?.string())
                    }

                })
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private fun sendNotification(title: String?, body: String){
        //어떤 모양으로 알림을 할지 설정한 다음 실제 폰 상단에 표시하도록 한다.
        //pendingIntent를 이용 알림을 클릭하면 열 앱의 액티비티를 설정해 준다.
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //createNotificationChannel은 안드로이드 8.0(오레오) 이상에서 알림 보내는 방법
            //IMPORTANCE_HIGH: 팝업알림,소리
            //IMPORTANCE_DEFAULT: 소리
            //IMPORTANCE_LOW: 무음
            //IMPORTANCE_MIN: 상태표시줄에 안보임
            val importance = NotificationManager.IMPORTANCE_HIGH //중요도 설정
            val mChannel =
                NotificationChannel("default", "기본채널", importance) //안드로이드 버전 Oreo  이상이면 사용 해야함
            notificationManager.createNotificationChannel(mChannel) //채널 생성
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this,"default")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setSound(defaultSound)
            .setAutoCancel(true) //알림창 터치시 자동으로 알림창을 닫음
            .setPriority(Notification.PRIORITY_HIGH) //우선순위
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, notificationBuilder.build())
    }

}