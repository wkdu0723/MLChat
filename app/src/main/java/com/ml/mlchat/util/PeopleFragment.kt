package com.ml.mlchat.util

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ml.mlchat.HomeActivity
import com.ml.mlchat.R
import com.ml.mlchat.UserActivity
import com.ml.mlchat.model.PeopleDTO
import kotlinx.android.synthetic.main.fragment_people.view.*
import kotlinx.android.synthetic.main.item_people.*
import kotlinx.android.synthetic.main.item_people.view.*


class PeopleFragment : Fragment() {
    var firestore : FirebaseFirestore? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_people, container, false)
        firestore = FirebaseFirestore.getInstance() //초기화
        view.peopleviewfragment_recyclerview.adapter = PeopleViewRecyclerViewAdapter()
        view.peopleviewfragment_recyclerview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class PeopleViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var peopleDTO : ArrayList<PeopleDTO> = arrayListOf()

       init {
            firestore?.collection("user")?.addSnapshotListener{querySnapshot, firebaseFirestoreException ->
                peopleDTO.clear()
                if(querySnapshot == null) return@addSnapshotListener

                for(snapshot in querySnapshot!!.documents){
                    var item = snapshot.toObject(PeopleDTO::class.java)
                    peopleDTO.add(item!!)
                }
                    notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(parent.context).inflate(R.layout.item_people,parent,false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)


        override fun getItemCount(): Int {
            return peopleDTO.size

        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewholder = (holder as CustomViewHolder).itemView
            var resourceId = R.mipmap.ic_launcher
            viewholder.item_title_textview.text = peopleDTO!![position].userName
            //Image(이미지 url을 load하기위해 Glide사용)
            if(peopleDTO!![position].imageUrl == null){
                Glide.with(holder.itemView.context).load(resourceId).into(viewholder.item_profile_imageview)
            }else{
                Glide.with(holder.itemView.context).load(peopleDTO!![position].imageUrl).into(viewholder.item_profile_imageview)
            }
            //viewholder.item_profile_imageview.text = peopleDTO!![position].userEmail


            //프로필을 클릭하면 상세페이지로 이동
            viewholder.item_profile_imageview.setOnClickListener{
                val intent = Intent(context, UserActivity::class.java)
                intent.putExtra("userName",peopleDTO[position].userName)
                intent.putExtra("destinationUid",peopleDTO[position].uid)
                startActivity(intent)
            }
        }

    }
}