package aero.cubox.communication.utils

import java.io.Serializable

data class EnrollBody(
    val deviceUuid: String,
//    val userId : String,
    val type: String,
    val os: String,
    val manufacture: String,
    val ip: String,
    val mac: String
):Serializable
