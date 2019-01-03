package com.example.p3.kotlinmessenger.messages

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.p3.kotlinmessenger.R
import com.example.p3.kotlinmessenger.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class NewMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        //ツールバーのタイトルを設定
        supportActionBar?.title = "Select User"
        fetchUsers()
    }

    companion object {
        val USER_KEY = "USER_KEY"
    }

    private fun fetchUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("/users/")
        //Firebaseから値を取得する際に使用するリスナー
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            val adapter = GroupAdapter<ViewHolder>()

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach {
                    Log.d("NewMessage", it.toString())
                    val user = it.getValue(User::class.java)
                    if (user != null) {
                        adapter.add(UserItem(user))
                    }
                }
                recyclerview_newmessage.adapter = adapter

                adapter.setOnItemClickListener { item, view ->
                    val userItem = item as UserItem
                    val intent = Intent(view.context, ChatLogActivity::class.java)

                    //intent.putExtra(USER_KEY,userItem.user.username)
                    //選択したユーザの情報を保持するクラスをインテントに詰め込む
                    intent.putExtra(USER_KEY, userItem.user)
                    startActivity(intent)
                    //一旦Activityを落とすことにする
                    finish()
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                TODO("")
            }
        })
    }
}

class UserItem(val user: User) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        //フェッチしたレコード毎に値を設定していく
        viewHolder.itemView.username_textview_new_message.text = user.username

        //ImageView等を指定し、リクエストした画像を当て込む先を指定する。
        Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.imageView_chat_to_row)
    }

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }
}