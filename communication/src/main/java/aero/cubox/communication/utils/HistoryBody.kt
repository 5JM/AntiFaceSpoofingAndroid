package aero.cubox.communication.utils

import java.io.Serializable

data class HistoryBody(
    val code : String,
    val value : String,
    val transactionId : String
):Serializable

