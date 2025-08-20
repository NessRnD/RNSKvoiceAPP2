package com.example.rnskvoiceapp

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.util.Date

class HistoryActivity : AppCompatActivity() {
    private lateinit var historyContainer: LinearLayout
    private lateinit var backButton: Button
    private var mediaPlayer: MediaPlayer? = null
    private val historyItems = mutableListOf<HistoryItem>()

    companion object {
        private const val TAG = "HistoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyContainer = findViewById(R.id.historyContainer)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            supportFinishAfterTransition()
        }

        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()
        historyItems.clear()
        historyItems.addAll(HistoryManager.getHistoryItems(this))

        if (historyItems.isEmpty()) {
            val emptyView = TextView(this).apply {
                text = "История записей пуста"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 100, 0, 0)
            }
            historyContainer.addView(emptyView)
            return
        }

        historyItems.forEachIndexed { index, item ->
            val historyItemView = layoutInflater.inflate(R.layout.item_history, null)

            historyItemView.findViewById<TextView>(R.id.historyDate).text = "${item.date} ${item.time}"
            historyItemView.findViewById<TextView>(R.id.historyCount).text = "Записей: ${item.recordCount}"
            historyItemView.findViewById<TextView>(R.id.historyFileName).text = item.fileName

            val playButton = historyItemView.findViewById<Button>(R.id.playButton)
            val stopButton = historyItemView.findViewById<Button>(R.id.stopButton)
            val shareButton = historyItemView.findViewById<Button>(R.id.shareButton)
            val deleteButton = historyItemView.findViewById<Button>(R.id.deleteButton)

            // Кнопка воспроизведения
            playButton.setOnClickListener {
                stopPlaying()
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(item.filePath)
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
                }
            }

            // Кнопка остановки
            stopButton.setOnClickListener {
                stopPlaying()
            }

            // Кнопка отправки
            shareButton.setOnClickListener {
                shareAudioFile(item.filePath)
            }

            // Кнопка удаления
            deleteButton.setOnClickListener {
                showDeleteConfirmation(item, index, historyItemView)
            }

            historyContainer.addView(historyItemView)
        }
    }

    private fun showDeleteConfirmation(item: HistoryItem, index: Int, itemView: View) {
        AlertDialog.Builder(this)
            .setTitle("Удаление записи")
            .setMessage("Вы уверены, что хотите удалить запись от ${item.date} ${item.time}?")
            .setPositiveButton("Удалить") { dialog, which ->
                if (HistoryManager.deleteHistoryItem(this, item)) {
                    // Удаляем view из контейнера
                    historyContainer.removeView(itemView)

                    // Удаляем из локального списка
                    historyItems.removeAt(index)

                    Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()

                    // Если записей не осталось, показываем сообщение
                    if (historyItems.isEmpty()) {
                        loadHistory() // Перезагружаем чтобы показать пустой экран
                    }
                } else {
                    Toast.makeText(this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun shareAudioFile(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
                return
            }

            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    this,
                    "${packageName}.provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Аудиозапись СК")
                putExtra(Intent.EXTRA_TEXT, "Аудиозапись от ${SimpleDateFormat("dd.MM.yyyy HH:mm").format(
                    Date()
                )}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Отправить аудиозапись через"))

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отправки: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onStop() {
        super.onStop()
        stopPlaying()
    }

}