package com.example.rnskvoiceapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object HistoryManager {
    private const val TAG = "HistoryManager"
    private const val PREFS_NAME = "AudioChecklistPrefs"
    private const val HISTORY_KEY = "audio_history"
    private val gson = Gson()


    fun addHistoryItem(context: Context, item: HistoryItem) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getHistoryItems(context).toMutableList()
        history.add(0, item)
        prefs.edit().putString(HISTORY_KEY, gson.toJson(history)).apply()
    }

    fun getHistoryItems(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(HISTORY_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun deleteHistoryItem(context: Context, item: HistoryItem): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val history = getHistoryItems(context).toMutableList()

            // Удаляем запись из списка
            val removed = history.removeAll { it.filePath == item.filePath }

            if (removed) {
                // Удаляем физический файл
                val file = File(item.filePath)
                if (file.exists()) {
                    file.delete()
                }

                // Сохраняем обновленный список
                prefs.edit().putString(HISTORY_KEY, gson.toJson(history)).apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления записи", e)
            false
        }
    }

    fun deleteHistoryItemByPath(context: Context, filePath: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val history = getHistoryItems(context).toMutableList()

            // Удаляем запись из списка
            val removed = history.removeAll { it.filePath == filePath }

            if (removed) {
                // Удаляем физический файл
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }

                // Сохраняем обновленный список
                prefs.edit().putString(HISTORY_KEY, gson.toJson(history)).apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка удаления записи", e)
            false
        }
    }
}