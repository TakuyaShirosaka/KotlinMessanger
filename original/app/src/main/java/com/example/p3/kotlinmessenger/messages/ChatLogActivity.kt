package com.example.p3.kotlinmessenger.messages

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.example.p3.kotlinmessenger.R
import com.example.p3.kotlinmessenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*


class ChatLogActivity : AppCompatActivity() {

    companion object {
        val TAG = "ChatLog"
    }

    //ListViewに表示する項目は「アダプター(Adapter)」と呼ばれるものをセットすることで表示されます。
    val adapter = GroupAdapter<ViewHolder>()
    var toUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //レイアウトのセット
        setContentView(R.layout.activity_chat_log)

        //AdapterというのはViewとデータの橋渡し役で、 Viewに必要なデータを渡してあげる役割があります。
        recyclerview_chat_log.adapter = adapter

        //intentにシリアライズ化して詰め込んだデータを取得する
        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username

        //メッセージの初回表示
        listenForMessages()

        //各種リスナー
        adapter.setOnItemClickListener { item, v ->
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // ソフトキーボードを閉じる
            inputMethodManager.hideSoftInputFromWindow(v?.windowToken, 0)
        }

        //送信ボタン
        send_button_chat_log.setOnClickListener {
            Log.d(TAG, "Attempt to send message....")
            performSendMessage()
            //キーボードを引っ込ませる
            val inputMethodMgr = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodMgr.hideSoftInputFromWindow(
                editText_chat_log.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        //フォーカスイベント
        editText_chat_log.setOnFocusChangeListener { v, hasFocus ->
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (hasFocus) {
                // ソフトキーボードを表示する
                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED)
            } else {
                // ソフトキーボードを閉じる
                inputMethodManager.hideSoftInputFromWindow(v?.windowToken, 0)
            }
        }
    }

    //FireBaseからmessageの受信を行う
    private fun listenForMessages() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            //アイテムのリストを取得するか、アイテムのリストへの追加がないかリッスンします。
            //onChildChanged() や onChildRemoved() と併用して、リストに対する変更をモニタリングすることをおすすめします。
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage = p0.getValue(ChatMessage::class.java)
                if (chatMessage != null) {
                    Log.d(TAG, "chatMessage:${chatMessage.text}")
                    Log.d(TAG, "chatMessage.fromId:${chatMessage.fromId}")

                    if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
                        //currentUserはコンパニオンオブジェクトのためアクセスできる
                        val currentUser = LatestMessagesActivity.currentUser ?: return
                        adapter.add(ChatFromItem(chatMessage.text, currentUser))
                    } else {
                        adapter.add(ChatToItem(chatMessage.text, toUser!!))
                    }
                }
                Log.d(TAG, "Finish read chatMessage")
                //一番下の投稿にフォーカスがあたるようにする
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildRemoved(p0: DataSnapshot) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }

    class ChatMessage(val id: String, val text: String, val fromId: String, val toId: String, val timestamp: Long) {
        constructor() : this("", "", "", "", -1)
    }

    private fun performSendMessage() {
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val text = editText_chat_log.text.toString()
        val toId = user.uid

        if (fromId == null || text.trim() == "") return

        //pushは新たにユニークキーを生成して、レコードを作る
        //送信元
        val referrence = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        //宛先
        val toReferrence = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()
        //メッセージデータ
        val chatMessage = ChatMessage(referrence.key!!, text, fromId, toId, System.currentTimeMillis() / 1000)

        referrence.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat messages:${referrence.key}")

                //入力欄をクリア
                editText_chat_log.text.clear()
                //一番下の投稿にフォーカスがあたるようにする
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

        toReferrence.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved your chat messages:${toReferrence.key}")
            }
        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)

    }
}

class ChatFromItem(val text: String, val user: User) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_textview_chat_from.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_chat_from_row
        Picasso.get().load(uri).into(targetImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}

class ChatToItem(val text: String, val user: User) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.username_textview_chat_to.text = text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageView_chat_to_row
        Picasso.get().load(uri).into(targetImageView)

    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}