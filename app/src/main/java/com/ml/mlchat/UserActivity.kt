package com.ml.mlchat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ml.mlchat.model.PeopleDTO
import kotlinx.android.synthetic.main.activity_user.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.bumptech.glide.RequestManager


class UserActivity : AppCompatActivity() {
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var firestore : FirebaseFirestore? = null
    var PICK_IMAGE_FROM_ALBUM = 0
    var userName : String? = null
    var uid : String? = null
    var currentUserUid : String? = null //자신의 아이디인지 상대방의 아이디인지 구분
    var auth : FirebaseAuth? = null



    lateinit var mGlideRequestManager : RequestManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)

        //initiate storage
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()
        //uid = FirebaseAuth.getInstance().currentUser?.uid
        auth = FirebaseAuth.getInstance() //초기화
        currentUserUid = auth?.currentUser?.uid
        userName = intent?.getStringExtra("userName")
        uid = intent?.getStringExtra("destinationUid")

        if (intent.hasExtra("userName")) {
            user_id_textview.text = userName
        } else {
            //Toast.makeText(this, "전달된 값이 없습니다", Toast.LENGTH_SHORT).show()
        }


        complete_textview.visibility = View.GONE //완료버튼 비활성화
        call_Imageview.visibility = View.GONE
        call_textview.visibility = View.GONE

        if(uid == currentUserUid){ //자신의 페이지(프로필 편집 기능 활성화)
            addphoto_image.visibility = View.VISIBLE //프로필 편집 버튼 활성화
            addphoto_textview.visibility = View.VISIBLE
            chat_imageview.visibility = View.GONE // 1:1채팅 버튼 비활성화
            chat_textview.visibility = View.GONE //1:1 채팅 텍스트 비활성화
            addphoto_image.setOnClickListener {
                addphoto_image.visibility = View.GONE //프로필 편집 버튼 비활성화
                addphoto_textview.visibility = View.GONE
                //open the album
                //미리보기 적용(완료버튼을 눌러야 최종 적용)
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(photoPickerIntent,PICK_IMAGE_FROM_ALBUM)
                complete_textview.visibility = View.VISIBLE //완료버튼 활성화
            }

            complete_textview.setOnClickListener {
                //사진 적용한 뒤 완료버튼 누르면 storage에 저장
                complete_textview.visibility = View.GONE
                contentUpload()
            }
        }else{ //상대 페이지 (1:1채팅기능 활성화)
            addphoto_image.visibility = View.GONE //프로필 편집 버튼 비활성화
            addphoto_textview.visibility = View.GONE //프로필 편집 텍스트 비활성화
            call_Imageview.visibility = View.VISIBLE
            call_textview.visibility = View.VISIBLE

            chat_imageview.setOnClickListener {
                //1:1 채팅액티비티로 이동
                Log.v("1:1채팅 시작","on")
                var intent = Intent(it.context,ChatActivity::class.java)
                intent.putExtra("currentUserUid",currentUserUid)
                intent.putExtra("destinationUid",uid)
                startActivity(intent)
            }
        }

        getProfileImage()
    }

    fun getProfileImage(){
        firestore?.collection("user")?.document(uid!!)?.addSnapshotListener{
                documentSnapshot, firebaseFirestoreException ->
            if(documentSnapshot == null) return@addSnapshotListener
            if(documentSnapshot.data != null){
                var peopleDTO = documentSnapshot.toObject(PeopleDTO::class.java)
                if(peopleDTO?.imageUrl != null){
                    var url = documentSnapshot?.data!!["imageUrl"] //이미지 주소값을 받아옴
                    mGlideRequestManager = Glide.with(getApplicationContext()) //with()에 getApplicationContext를 쓰지않으면 오류발생
                    mGlideRequestManager.load(url).apply(RequestOptions().circleCrop()).into(this?.user_imageview!!)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                //This is path to the selected image
                photoUri = data?.data
                user_imageview.setImageURI(photoUri)

            }else{
                //Exit the addPhotoActivity if you leave the album without selecting it
                finish()
            }
        }
    }

    fun contentUpload(){
        //Make filename
        var timestamp = SimpleDateFormat("yyyMMDdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

        //FileUpload
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->

                var peopleDTO = PeopleDTO()
                peopleDTO.uid = uid
                peopleDTO.imageUrl = uri.toString()
                peopleDTO.userName = userName
                peopleDTO.userEmail = FirebaseAuth.getInstance().currentUser?.email
                peopleDTO.timestamp = System.currentTimeMillis()

                //Insert downloadUrl of image
                firestore?.collection("user")?.document(uid!!)?.set(peopleDTO)

                setResult(Activity.RESULT_OK)
                finish()
            }
        }

    }
}
