package com.ml.mlchat.model

data class ChatDTO (
    var users : MutableMap<String, Boolean> = HashMap(),
    var comments : MutableMap<String, Comment> = HashMap()
){
    data class Comment(
        var userName : String? = null,
        var uid : String? = null,
        var message : String? = null,
        var imageUrl : String? = null

        )
}
