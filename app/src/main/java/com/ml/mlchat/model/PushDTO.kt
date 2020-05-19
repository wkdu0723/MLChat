package com.ml.mlchat.model


data class PushDTO(
    var to : String? = null,
    var notification : Notification = Notification()
){
    data class Notification( //푸시메세지 제목 및 내용
        var body : String? = null,
        var title : String? = null
    )
}