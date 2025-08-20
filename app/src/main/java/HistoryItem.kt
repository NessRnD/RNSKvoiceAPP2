package com.example.rnskvoiceapp
import java.io.File
import java.io.Serializable

data class HistoryItem(
    val date: String,
    val time: String,
    val filePath: String,
    val recordCount: Int,
    val fileSize: Long
) : Serializable {
    val fileName: String
        get() = File(filePath).name
}