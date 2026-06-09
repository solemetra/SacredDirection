package com.example.qiblaapp2

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DuaActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var btnPlayDua: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dua)
        TabUiHelper.applyBottomNavInsets(this)
        TabUiHelper.highlightBottomTab(this, TabUiHelper.BottomTab.DUA)
        setupNavigation()
        setupPlayButton()
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

    private fun setPlayButtonPlaying(playing: Boolean) {
        btnPlayDua.setBackgroundResource(
            if (playing) R.drawable.neo_dua_play_active else R.drawable.neo_fab_button
        )
    }

    private fun playSound() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.dua)

            if (mediaPlayer == null) {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show()
                return
            }

            setPlayButtonPlaying(true)

            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
                setPlayButtonPlaying(false)
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            setPlayButtonPlaying(false)
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
        if (::btnPlayDua.isInitialized) {
            setPlayButtonPlaying(false)
        }
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
