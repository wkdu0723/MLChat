package com.ml.mlchat

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.ml.mlchat.model.AlarmDTO
import com.ml.mlchat.model.ChatDTO
import com.ml.mlchat.util.FcmPush
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_message.view.*

class ChatActivity : AppCompatActivity() {
    var uid : String?  = null
    var destinationUid : String? = null
    var firestore : FirebaseFirestore? = null
    var chatRoomUid : String? = null
    var firebaseDatabase : FirebaseDatabase? = null
    var imageUrl : String? = null
    var userName : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        //상태바 흰색, 아이콘 검은색
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }

        firestore = FirebaseFirestore.getInstance()
        uid = intent.getStringExtra("currentUserUid")
        destinationUid = intent.getStringExtra("destinationUid")
        imageUrl = intent.getStringExtra("imageUrl")



        //자신의 닉네임을 데이터베이스에서 가져오기
        firestore?.collection("user")?.document(uid!!)?.get()?.addOnSuccessListener { document ->
            if (document != null) {
                userName = document.data!!["userName"] as String? //유저 이름 값 가져옴
            } else {

            }
        }

        chat_btn_send.setOnClickListener {
            var chatDTO = ChatDTO()
            chatDTO.users.put(uid!!,true)
            chatDTO.users.put(destinationUid!!,true)

            if(chatRoomUid == null){
                chat_btn_send.isEnabled = false //서버에 전송되기전에 채팅방 만들어지는것을 방지하기 위함
                //채팅방 생성
                FirebaseDatabase.getInstance().reference.child("chatrooms").push().setValue(chatDTO).addOnSuccessListener {
                    checkChatRoom()
                }
            }else{
                var comment = ChatDTO.Comment()
                comment.uid = uid
                comment.message = chat_edit_message.text.toString()
                comment.userName = userName
                FirebaseDatabase.getInstance().reference.child("chatrooms").child(chatRoomUid!!).child("comments").push()
                    .setValue(comment) //기존에 존재하는 채팅방에 입장
                fcmPushMessge(destinationUid!!, comment.message!!)

            }
        }

        checkChatRoom()
    }

    fun fcmPushMessge(destinationUid : String, message : String){
        var alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        //FcmPush(alarmDTO.destinationUid!!,"ML.Talk",alarmDTO.message!!)
        FcmPush.instance.sendMessage(alarmDTO.destinationUid!!,"ML.Talk",alarmDTO.message!!)
    }

    fun checkChatRoom(){
        FirebaseDatabase.getInstance().reference.child("chatrooms").orderByChild("users/"+uid).equalTo(true).
            addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(p0: DataSnapshot) {
                    for (snapshot in p0.children) {
                        var chatDTO = snapshot.getValue(ChatDTO::class.java)
                        if(chatDTO?.users!!.containsKey(destinationUid)){
                            chatRoomUid = snapshot.key
                            chat_btn_send.isEnabled = true
                            chat_recyclerview.adapter = ChatRecyclerviewAdapter()
                            chat_recyclerview.layoutManager = LinearLayoutManager(parent)

                        }
                    }
                }
            })
    }

    override fun onBackPressed() {
        // 뒤로가기 버튼 클릭
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }



    inner class ChatRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var chatDTOList : ArrayList<ChatDTO.Comment> = arrayListOf()

        init {
            FirebaseDatabase.getInstance().reference.child("users").child(destinationUid!!).addListenerForSingleValueEvent(
                object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        getMessageList()
                    }
                })
        }


        fun getMessageList(){
            FirebaseDatabase.getInstance().reference.child("chatrooms").child(chatRoomUid!!).child("comments").
                addValueEventListener(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        chatDTOList.clear()
                        for (snapshot in p0.children) {
                            chatDTOList.add(snapshot.getValue(ChatDTO.Comment::class.java)!!)
                        }
                        notifyDataSetChanged()
                    }
                })
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)

            return MessageViewHolder(view)
         }

        inner class MessageViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)


        override fun getItemCount(): Int {
            return chatDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as MessageViewHolder).itemView

            if(chatDTOList[position].uid.equals(uid)){
                viewholder.item_message_textview.right
                viewholder.item_message_textview.text = chatDTOList[position].message
                viewholder.item_message_textview.setBackgroundResource(R.drawable.rightbubble)
                viewholder.item_message_linearlayout.visibility = View.GONE
                viewholder.item_message_linearlayout_main.gravity = Gravity.RIGHT
            }else{
                firestore?.collection("user")?.document(chatDTOList!![position].uid!!)?.addSnapshotListener{
                        documentSnapshot, firebaseFirestoreException ->
                    if(documentSnapshot == null) return@addSnapshotListener
                    if(documentSnapshot.data != null){
                        var url = documentSnapshot?.data!!["imageUrl"] //이미지 주소값을 받아옴
                        Glide.with(holder.itemView.context).load(url).apply(RequestOptions().circleCrop())
                            .into(viewholder.messageItem_imageview_profile)
                    }
                }
                viewholder.messageItem_textview_name.setText(chatDTOList[position].userName)
                viewholder.item_message_textview.text = chatDTOList[position].message
                viewholder.item_message_textview.setBackgroundResource(R.drawable.leftbubble)
                viewholder.item_message_linearlayout.visibility = View.VISIBLE
                viewholder.item_message_linearlayout_main.gravity = Gravity.LEFT
            }
        }
    }


}
