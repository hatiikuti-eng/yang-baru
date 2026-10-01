package com.example.voicebridge

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        tts = TextToSpeech(this, this)

        findViewById<Button>(R.id.voiceButton).setOnClickListener {
            startVoiceInput()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("id", "ID")
        }
    }

    private fun startVoiceInput() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ucapkan perintah...")
        }
        startActivityForResult(intent, 101)
    }

    @Deprecated("Use Activity Result APIs in a future refactor.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 101 || resultCode != RESULT_OK) return

        val command = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            ?: return
        statusText.text = "Perintah: $command"
        confirmCommand(command)
    }

    private fun confirmCommand(command: String) {
        speak("Saya memahami: $command. Apakah saya jalankan?")

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi")
            .setMessage(command)
            .setPositiveButton("Jalankan") { _, _ -> executeCommand(command) }
            .setNegativeButton("Batal") { _, _ -> speak("Dibatalkan") }
            .show()
    }

    private fun executeCommand(command: String) {
        val lower = command.lowercase(Locale.getDefault())
        val packageName = when {
            "whatsapp" in lower -> "com.whatsapp"
            "chrome" in lower -> "com.android.chrome"
            "youtube" in lower -> "com.google.android.youtube"
            "pengaturan" in lower || "settings" in lower -> null
            else -> null
        }

        try {
            val intent = if (packageName == null && ("pengaturan" in lower || "settings" in lower)) {
                Intent(android.provider.Settings.ACTION_SETTINGS)
            } else {
                packageManager.getLaunchIntentForPackage(packageName ?: "")
                    ?: throw IllegalArgumentException("Aplikasi tidak ditemukan")
            }
            startActivity(intent)
            speak("Perintah dijalankan")
        } catch (e: Exception) {
            speak("Maaf, perintah belum didukung")
            statusText.text = "Belum didukung: $command"
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_bridge")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
