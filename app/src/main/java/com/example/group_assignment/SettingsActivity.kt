package com.example.group_assignment

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.group_assignment.repository.SettingsRepository
import kotlinx.coroutines.launch
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsRepo = SettingsRepository(this)
        val switchDarkMode = findViewById<Switch>(R.id.switchDarkMode)
        val btnBackup = findViewById<Button>(R.id.btnBackup)

        lifecycleScope.launch {
            settingsRepo.isDarkMode.collect { isDark ->
                switchDarkMode.isChecked = isDark
            }
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch { settingsRepo.setDarkMode(isChecked) }
        }

        btnBackup.setOnClickListener {
            exportTasks()
        }
    }

    private fun exportTasks() {
        try {
            // 1. Setup nama fail unik (ada timestamp)
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "TaskMaster_Backup_$timestamp.json"

            // 2. Baca data dari fail internal (tasks.json)
            val file = java.io.File(filesDir, "tasks.json")
            if (!file.exists()) {
                Toast.makeText(this, "No data to export! Add tasks first.", Toast.LENGTH_SHORT).show()
                return
            }
            val content = file.readText()

            // 3. Setup MediaStore (Untuk simpan ke folder Documents secara Public)
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                // Simpan dalam folder Documents/TaskMasterBackups
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/TaskMasterBackups")
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // Bagitahu Android kita tengah tulis
                }
            }

            // 4. Tulis fail
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let { targetUri ->
                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }

                // 5. Release file (Dah siap tulis)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(targetUri, contentValues, null, null)
                }

                Toast.makeText(this, "Success! Saved to Documents/TaskMasterBackups/$fileName", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Error: Could not create file", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Export Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}