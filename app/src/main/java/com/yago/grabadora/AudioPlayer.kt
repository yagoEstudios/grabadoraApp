package com.yago.grabadora

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import java.util.Locale

/** Controlador simple de reproducción ligado a un botón play/pausa + SeekBar. */
class AudioPlayer(
    private val context: Context,
    private val playPause: ImageButton,
    private val seek: SeekBar,
    private val curText: TextView?,
    private val durText: TextView?
) {
    private var mp: MediaPlayer? = null
    private val h = Handler(Looper.getMainLooper())

    val isLoaded: Boolean get() = mp != null

    private val tick = object : Runnable {
        override fun run() {
            val p = mp ?: return
            if (p.isPlaying) {
                seek.progress = p.currentPosition
                curText?.text = fmt(p.currentPosition.toLong())
                h.postDelayed(this, 200)
            }
        }
    }

    init {
        playPause.setOnClickListener { toggle() }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mp?.seekTo(progress)
                    curText?.text = fmt(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    fun load(uri: Uri) {
        release()
        try {
            val p = MediaPlayer()
            p.setDataSource(context, uri)
            p.prepare()
            mp = p
            seek.max = p.duration
            durText?.text = fmt(p.duration.toLong())
            curText?.text = fmt(0)
            p.setOnCompletionListener {
                seek.progress = 0
                curText?.text = fmt(0)
                playPause.setImageResource(R.drawable.ic_play)
            }
            play()
        } catch (e: Exception) {
            mp?.release(); mp = null
        }
    }

    fun play() {
        mp?.start()
        playPause.setImageResource(R.drawable.ic_pause)
        h.post(tick)
    }

    fun pause() {
        mp?.pause()
        playPause.setImageResource(R.drawable.ic_play)
    }

    fun toggle() {
        val p = mp ?: return
        if (p.isPlaying) pause() else play()
    }

    fun release() {
        h.removeCallbacks(tick)
        mp?.release()
        mp = null
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }
}
