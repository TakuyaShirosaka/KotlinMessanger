package com.example.p3.kotlinmessenger.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//ユーザー情報クラス
//ユーザーID、ユーザー名、画像ファイルURL
//AndroidでIntentにデータを詰め込む際に、オブジェクトをシリアライズする必要があります。
//その際に、Parcelableインターフェースを実装することで格納することが可能になります。
@Parcelize
class User(val uid: String, val username: String, val profileImageUrl: String) : Parcelable {
    constructor() : this("", "", "")
}