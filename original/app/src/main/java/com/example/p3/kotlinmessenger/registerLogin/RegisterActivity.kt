package com.example.p3.kotlinmessenger.registerLogin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.example.p3.kotlinmessenger.R
import com.example.p3.kotlinmessenger.messages.LatestMessagesActivity
import com.example.p3.kotlinmessenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {
    //companion：static変数、メソッドの代わり
    companion object {
        val TAG = "Register Activity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        register_button_register.setOnClickListener {
            performRegister()
        }

        already_have_account_test_view.setOnClickListener {
            Log.d(TAG, "Try to show Login Activity")

            //::class.javaでクラスオブジェクトの参照
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        selectphoto_button_register.setOnClickListener {
            Log.d(TAG, "Try to show photo selector")
            //ユーザーが連絡先を選択し、アプリが連絡先情報にアクセスできるようにする
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

    }

    //varの場合、変更が可能
    var selectedPhotoUri: Uri? = null

    //画像取得の終了後に来る処理
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Photo was selected")

            //Bitmapで画像を取得、フォームの周りに黒線もひいとく
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)

            selectphoto_imageview_register.setImageBitmap(bitmap)
            selectphoto_button_register.alpha = 0f

        }
    }

    //Registerボタン押下！
    private fun performRegister() {
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()

        //emailとpwの入力チェック
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in emai/pw", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Email is: " + email)
        Log.d(TAG, "Password: $password")

        //サインアップ
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener

                //else if Successful
                Log.d(TAG, "Successfully created user with uid: ${it.result?.user?.uid}")
                uploadImageToFireBaseStorage()
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to create User: ${it.message}")
                Toast.makeText(this, "Failed to create User: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    //ストレージへのプロフィール写真の保存、ユーザー情報をDBへ保存
    //images配下に採番されたファイルIDで登録を行う
    private fun uploadImageToFireBaseStorage() {
        if (selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                Log.d(TAG, "Successfuly uploaded image:${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d(TAG, "File Location :$it")

                    //画像の保存が完了したタイミングでDBへの登録も行う
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener {
                //しくじった時、考えないようにする
            }
    }

    //Realtime Databaseにデータを格納
    //users配下にユーザーID単位で登録する
    //登録完了後に画面遷移させる
    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, username_edittext_register.text.toString(), profileImageUrl)

        //Intent.FLAG_ACTIVITY_CLEAR_TASK:Activityのスタックを削除して起動する
        //Intent.FLAG_ACTIVITY_NEW_TASK:タスクがスタックに存在しても新しいタスクとして起動

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d(TAG, "Finally we saved the user to Firebase Database")
                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .addOnFailureListener {
                Log.d(TAG, "Failed to set value to Database")
            }
    }
}
