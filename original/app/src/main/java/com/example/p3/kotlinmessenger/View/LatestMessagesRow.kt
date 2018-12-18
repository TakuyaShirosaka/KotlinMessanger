package com.example.p3.kotlinmessenger.View

import android.util.Log
import com.example.p3.kotlinmessenger.R
import com.example.p3.kotlinmessenger.messages.ChatLogActivity
import com.example.p3.kotlinmessenger.messages.LatestMessagesActivity
import com.example.p3.kotlinmessenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class LatestMessageRow(val chatMessage: ChatLogActivity.ChatMessage) : Item<ViewHolder>() {
    var chatPartnerUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.message_textView_latest_message.text = chatMessage.text
        val chatPartnerId: String
        if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
            chatPartnerId = chatMessage.toId
        } else {
            chatPartnerId = chatMessage.fromId
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")

        Log.d(LatestMessagesActivity.TAG, "ref:$ref")
        //recycleviewで使ってるやつとの差は、こちらは一回データを取ってくるだけという点(データの追加・変更の監視はしない）
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            //パスのコンテンツ全体に対する変更の読み取りとリッスンを行います。
            override fun onDataChange(p0: DataSnapshot) {
                chatPartnerUser = p0.getValue(User::class.java) ?: return

                viewHolder.itemView.username_textView_latest_message.text = chatPartnerUser?.username
                val url: String? = chatPartnerUser?.profileImageUrl
                val targetImageView = viewHolder.itemView.imageView_latest_message
                Picasso.get().load(url).into(targetImageView)
            }
        })
    }

    override fun getLayout(): Int {
        return R.layout.latest_message_row
    }
}