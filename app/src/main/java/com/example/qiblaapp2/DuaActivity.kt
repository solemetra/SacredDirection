package com.example.qiblaapp2

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DuaActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var btnPlayDua: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dua)
        TabUiHelper.applyBottomNavInsets(this)
        highlightActiveTab()
        setupNavigation()
        setupPlayButton()
    }

    private fun highlightActiveTab() {
        findViewById<LinearLayout>(R.id.btnDua).findViewById<ImageView>(R.id.iconDua)
            ?.setColorFilter(ContextCompat.getColor(this, R.color.blue_primary))
        findViewById<LinearLayout>(R.id.btnDirection).findViewById<ImageView>(R.id.iconDirection)
            ?.setColorFilter(ContextCompat.getColor(this, R.color.gray_text))
        findViewById<LinearLayout>(R.id.btnPrayerTimes).findViewById<ImageView>(R.id.iconPrayer)
            ?.setColorFilter(ContextCompat.getColor(this, R.color.gray_text))
        findViewById<LinearLayout>(R.id.btnSettings).findViewById<ImageView>(R.id.iconSettings)
            ?.setColorFilter(ContextCompat.getColor(this, R.color.gray_text))
    }

    private fun setupNavigation() {
        findViewById<LinearLayout>(R.id.btnDirection).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.btnPrayerTimes).setOnClickListener {
            startActivity(Intent(this, PrayerTimesActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun setupPlayButton() {
        btnPlayDua = findViewById(R.id.btnPlayDua)
        btnPlayDua.setOnClickListener {
            try {
                playSound()
            } catch (e: Exception) {
                Toast.makeText(this, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playSound() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.dua)

            if (mediaPlayer == null) {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show()
                return
            }

            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            Toast.makeText(this, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSound() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
            // Ignore
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }

    override fun onPause() {
        stopSound()
        super.onPause()
    }
}
