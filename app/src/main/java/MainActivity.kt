package com.example.rnskvoiceapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var recordButtons: List<Button>
    private lateinit var playButtons: List<Button>
    private lateinit var deleteButtons: List<Button>
    private lateinit var saveButton: Button
    private lateinit var historyButton: Button

    private var currentRecordingIndex: Int = -1
    private var currentPlayingIndex: Int = -1
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val audioFiles = arrayOfNulls<String>(8)

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ALL_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация кнопок записи
        recordButtons = listOf(
            findViewById(R.id.recordButton1),
            findViewById(R.id.recordButton2),
            findViewById(R.id.recordButton3),
            findViewById(R.id.recordButton4),
            findViewById(R.id.recordButton5),
            findViewById(R.id.recordButton6),
            findViewById(R.id.recordButton7),
            findViewById(R.id.recordButton8)/*,
            findViewById(R.id.recordButton9),
            findViewById(R.id.recordButton10),
            findViewById(R.id.recordButton11),
            findViewById(R.id.recordButton12),
            findViewById(R.id.recordButton13),
            findViewById(R.id.recordButton14),
            findViewById(R.id.recordButton15)*/
        )

        // Инициализация кнопок воспроизведения
        playButtons = listOf(
            findViewById(R.id.playButton1),
            findViewById(R.id.playButton2),
            findViewById(R.id.playButton3),
            findViewById(R.id.playButton4),
            findViewById(R.id.playButton5),
            findViewById(R.id.playButton6),
            findViewById(R.id.playButton7),
            findViewById(R.id.playButton8)/*,
            findViewById(R.id.playButton9)*//*,
            findViewById(R.id.playButton10),
            findViewById(R.id.playButton11),
            findViewById(R.id.playButton12),
            findViewById(R.id.playButton13),
            findViewById(R.id.playButton14),
            findViewById(R.id.playButton15)*/
        )

        // Инициализация кнопок удаления
        deleteButtons = listOf(
            findViewById(R.id.deleteButton1),
            findViewById(R.id.deleteButton2),
            findViewById(R.id.deleteButton3),
            findViewById(R.id.deleteButton4),
            findViewById(R.id.deleteButton5),
            findViewById(R.id.deleteButton6),
            findViewById(R.id.deleteButton7),
            findViewById(R.id.deleteButton8)/*,
            findViewById(R.id.deleteButton9),
            findViewById(R.id.deleteButton10),
            findViewById(R.id.deleteButton11),
            findViewById(R.id.deleteButton12),
            findViewById(R.id.deleteButton13),
            findViewById(R.id.deleteButton14),
            findViewById(R.id.deleteButton15)*/
        )

        saveButton = findViewById(R.id.saveButton)
        historyButton = findViewById(R.id.historyButton)

        setupButtons()

        // Проверяем разрешения при запуске
        if (!checkAllPermissions()) {
            requestAllPermissions()
        }
    }

    private fun setupButtons() {
        // Настройка кнопок записи
        recordButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                it.hideKeyboard()
                if (currentRecordingIndex == index && mediaRecorder != null) {
                    stopRecording(index)
                } else {
                    if (currentRecordingIndex != -1 && mediaRecorder != null) {
                        stopRecording(currentRecordingIndex)
                    }
                    startRecording(index)
                }
            }
        }

        // Настройка кнопок воспроизведения
        playButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                it.hideKeyboard()
                if (currentPlayingIndex == index && mediaPlayer?.isPlaying == true) {
                    stopPlaying()
                } else {
                    if (currentPlayingIndex != -1) {
                        stopPlaying()
                    }
                    startPlaying(index)
                }
            }
        }

        // Настройка кнопок удаления
        deleteButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                it.hideKeyboard()
                deleteRecording(index)
            }
        }

        saveButton.setOnClickListener {
            it.hideKeyboard()
            if (prepareForSave()) {
                mergeAudioFilesWithSilence()
            }
        }

        historyButton.setOnClickListener {
            it.hideKeyboard()
            val intent = Intent(this@MainActivity, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun startRecording(index: Int) {
        currentRecordingIndex = index
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val audioFileName = "AUDIO_${index}_$timeStamp.3gp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val audioFilePath = File(storageDir, audioFileName).absolutePath

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            audioFiles[index] = audioFilePath

            // Обновляем UI
            recordButtons[index].text = "Стоп запись ${index + 1}"
            recordButtons[index].setBackgroundColor(
                ContextCompat.getColor(this, R.color.recording_red)
            )
            playButtons[index].isEnabled = false
            deleteButtons[index].isEnabled = false

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи", e)
            Toast.makeText(this, "Ошибка записи: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording(index: Int) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка остановки записи", e)
        } finally {
            mediaRecorder = null
            currentRecordingIndex = -1

            // Обновляем UI после успешной записи
            if (audioFiles[index] != null) {
                recordButtons[index].text = "Перезапись ${index + 1}"
                recordButtons[index].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.recorded_green)
                )
                playButtons[index].isEnabled = true
                deleteButtons[index].isEnabled = true
            } else {
                recordButtons[index].text = "Запись ${index + 1}"
                recordButtons[index].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.not_recorded_gray)
                )
                playButtons[index].isEnabled = false
                deleteButtons[index].isEnabled = false
            }
        }
    }

    private fun startPlaying(index: Int) {
        val filePath = audioFiles[index] ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                setOnCompletionListener {
                    stopPlaying()
                }
                prepare()
                start()
            }
            currentPlayingIndex = index

            // Обновляем UI
            playButtons[index].text = "⏹"
            recordButtons[index].isEnabled = false
            deleteButtons[index].isEnabled = false

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка воспроизведения", e)
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.release()
        mediaPlayer = null

        if (currentPlayingIndex != -1) {
            // Восстанавливаем UI
            playButtons[currentPlayingIndex].text = "▶"
            recordButtons[currentPlayingIndex].isEnabled = true
            deleteButtons[currentPlayingIndex].isEnabled = true
            currentPlayingIndex = -1
        }
    }

    private fun deleteRecording(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удаление записи")
            .setMessage("Вы уверены, что хотите удалить эту запись?")
            .setPositiveButton("Удалить") { _, _ ->
                // Удаляем файл
                audioFiles[index]?.let { filePath ->
                    try {
                        File(filePath).delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка удаления файла", e)
                    }
                }

                // Очищаем запись
                audioFiles[index] = null

                // Обновляем UI
                recordButtons[index].text = "Запись ${index + 1}"
                recordButtons[index].setBackgroundColor(
                    ContextCompat.getColor(this, R.color.not_recorded_gray)
                )
                playButtons[index].isEnabled = false
                deleteButtons[index].isEnabled = false

                Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // Остальные методы (checkAllPermissions, requestAllPermissions, onRequestPermissionsResult,
    // showPermissionExplanation, openAppSettings, prepareForSave, mergeAudioFilesWithSilence,
    // saveToHistory, resetRecordingUI, onStop) остаются без изменений из предыдущего кода

    private fun checkAllPermissions(): Boolean {
        val hasRecordPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        return hasRecordPermission && hasStoragePermission
    }

    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Запись аудио
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        // Хранилище (для разных версий Android)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            // Для Android 13+ также может понадобиться POST_NOTIFICATIONS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android до 13
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_ALL_PERMISSIONS // Используем единый код
            )
        } else {
            // Все разрешения уже есть
            Toast.makeText(this, "Все разрешения предоставлены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            // Проверяем все ли разрешения получены
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Toast.makeText(this, "Все разрешения получены!", Toast.LENGTH_SHORT).show()
            } else {
                // Проверяем какие именно разрешения не получены
                val deniedPermissions = mutableListOf<String>()

                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permission)
                    }
                }

                Log.d(TAG, "Отклоненные разрешения: $deniedPermissions")

                // Показываем объяснение только для критичных разрешений
                if (deniedPermissions.any { it == Manifest.permission.RECORD_AUDIO } ||
                    deniedPermissions.any { it == Manifest.permission.WRITE_EXTERNAL_STORAGE } ||
                    deniedPermissions.any { it == Manifest.permission.READ_MEDIA_AUDIO }) {
                    showPermissionExplanation()
                }
            }
        }
    }

    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Необходимые разрешения")
            .setMessage("Для работы приложения необходимы:\n\n" +
                    "• Доступ к микрофону - для записи аудио\n" +
                    "• Доступ к хранилищу - для сохранения записей\n\n" +
                    "Без этих разрешений приложение не сможет работать корректно.")
            .setPositiveButton("Предоставить разрешения") { _, _ ->
                // Запрашиваем again
                requestAllPermissions()
            }
            .setNegativeButton("Настройки") { _, _ ->
                openAppSettings()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun prepareForSave(): Boolean {
        // Проверяем есть ли записи
        if (audioFiles.none { it != null }) {
            Toast.makeText(this, "Сначала сделайте записи", Toast.LENGTH_SHORT).show()
            return false
        }

        // Проверяем разрешения
        if (!checkAllPermissions()) {
            // Показываем диалог с объяснением
            AlertDialog.Builder(this)
                .setTitle("Требуются разрешения")
                .setMessage("Для сохранения записей нужен доступ к хранилищу устройства.")
                .setPositiveButton("Предоставить") { _, _ ->
                    requestAllPermissions()
                }
                .setNegativeButton("Отмена", null)
                .show()
            return false
        }

        return true
    }

    private fun mergeAudioFilesWithSilence() {
        val validFiles = audioFiles.filterNotNull().map { File(it) }
        if (validFiles.isEmpty()) {
            Toast.makeText(this, "Нет записей для объединения", Toast.LENGTH_SHORT).show()
            return
        }

        val outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return
        if (!outputDir.exists()) outputDir.mkdirs()

        val outputFile = File(outputDir,
            "MERGED_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.m4a")

        try {
            validFiles.forEach { file ->
                if (!file.exists()) {
                    runOnUiThread {
                        Toast.makeText(this, "Файл ${file.name} не найден", Toast.LENGTH_LONG).show()
                    }
                    return
                }
            }

            val listFile = File(cacheDir, "concat_list_${System.currentTimeMillis()}.txt").apply {
                deleteOnExit()
            }

            FileWriter(listFile).use { writer ->
                validFiles.forEach { file ->
                    writer.write("file '${file.absolutePath}'\n")
                }
            }

            val cmd = StringBuilder().apply {
                append("-y ")
                append("-f concat ")
                append("-safe 0 ")
                append("-i \"${listFile.absolutePath}\" ")
                append("-filter_complex \"[0:a]apad=pad_dur=1[out]\" ")
                append("-map \"[out]\" ")
                append("-c:a aac ")
                append("-b:a 128k ")
                append("\"${outputFile.absolutePath}\"")
            }.toString()

            Log.d(TAG, "FFmpeg command: $cmd")

            FFmpegKit.executeAsync(cmd, { session ->
                Log.d(TAG, "FFmpeg session completed: ${session.returnCode}")
                Log.d(TAG, "FFmpeg logs: ${session.allLogsAsString}")

                if (ReturnCode.isSuccess(session.returnCode)) {
                    if (outputFile.exists() && outputFile.length() > 0) {
                        runOnUiThread {
                            saveToHistory(outputFile, validFiles.size)
                            resetRecordingUI()
                            Toast.makeText(
                                this@MainActivity,
                                "Записи успешно объединены!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка: выходной файл не создан",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Ошибка объединения. Код: ${session.returnCode}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при объединении", e)
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Фатальная ошибка: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun saveToHistory(outputFile: File, count: Int) {
        try {
            val historyItem = HistoryItem(
                date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()),
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                filePath = outputFile.absolutePath,
                recordCount = count,
                fileSize = outputFile.length() / 1024
            )
            HistoryManager.addHistoryItem(this, historyItem)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка сохранения в историю", e)
        }
    }

    private fun resetRecordingUI() {
        audioFiles.fill(null)
        recordButtons.forEachIndexed { index, button ->
            button.text = "Запись ${index + 1}"
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.not_recorded_gray))
            playButtons[index].isEnabled = false
            deleteButtons[index].isEnabled = false
        }
    }

    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
        mediaRecorder = null
        stopPlaying()
    }
}
