package com.ml.mlchat.model

data class AlarmDTO(
    var destinationUid : String? = null,
    var userId : String? = null,
    var uid : String? = null,
    var message : String? = null,
    var timestamp : Long? = null
)