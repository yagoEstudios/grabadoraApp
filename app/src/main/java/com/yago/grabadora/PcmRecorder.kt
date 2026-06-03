package com.yago.grabadora

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.abs

/** Graba PCM 16-bit mono a un fichero WAV válido, con pausa/reanudar. */
class PcmRecorder(
    private val sampleRate: Int,
    private val onAmplitude: (Float) -> Unit,
    private val onElapsed: (Long) -> Unit
) {
    private val channels = 1
    private val bits = 16

    @Volatile private var running = false
    private var record: AudioRecord? = null
    private var worker: Thread? = null
    private var raf: RandomAccessFile? = null
    var file: File? = null
        private set

    private val main = Handler(Looper.getMainLooper())

    val isRecording get() = running

    @SuppressLint("MissingPermission")
    fun start(outFile: File) {
        file = outFile
        val r = RandomAccessFile(outFile, "rw")
        r.setLength(0)
        writeHeader(r, 0)
        raf = r
        beginCapture()
    }

    fun resume() {
        if (!running && raf != null) beginCapture()
    }

    fun pause() {
        if (!running) return
        stopCapture()
        patchHeader()
    }

    fun finish(): File? {
        if (running) stopCapture()
        patchHeader()
        try { raf?.close() } catch (_: Exception) {}
        raf = null
        return file
    }

    fun discard() {
        stopCapture()
        try { raf?.close() } catch (_: Exception) {}
        raf = null
        file?.delete()
        file = null
    }

    fun elapsedMs(): Long {
        val len = (raf?.length() ?: 44L) - 44L
        return if (len <= 0) 0 else len * 1000L / (sampleRate.toLong() * channels * bits / 8)
    }

    private fun stopCapture() {
        running = false
        try { record?.stop() } catch (_: Exception) {}
        worker?.join()
        worker = null
        record?.release()
        record = null
    }

    @SuppressLint("MissingPermission")
    private fun beginCapture() {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, sampleRate)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release(); return
        }
        record = rec
        running = true
        rec.startRecording()
        worker = thread {
            val buf = ByteArray(bufSize)
            val r = raf ?: return@thread
            try { r.seek(r.length()) } catch (_: Exception) {}
            var lastTenth = -1L
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    try { r.write(buf, 0, n) } catch (_: Exception) {}
                    val amp = peak(buf, n)
                    val ms = elapsedMs()
                    main.post { onAmplitude(amp) }
                    if (ms / 100 != lastTenth) {
                        lastTenth = ms / 100
                        main.post { onElapsed(ms) }
                    }
                }
            }
        }
    }

    private fun patchHeader() {
        val r = raf ?: return
        try {
            val dataLen = r.length() - 44
            r.seek(4); r.write(i32(36 + dataLen))
            r.seek(40); r.write(i32(dataLen))
            r.seek(r.length())
        } catch (_: Exception) {}
    }

    private fun writeHeader(r: RandomAccessFile, dataLen: Long) {
        val byteRate = (sampleRate * channels * bits / 8).toLong()
        r.seek(0)
        r.write("RIFF".toByteArray()); r.write(i32(36 + dataLen)); r.write("WAVE".toByteArray())
        r.write("fmt ".toByteArray()); r.write(i32(16)); r.write(i16(1)); r.write(i16(channels))
        r.write(i32(sampleRate.toLong())); r.write(i32(byteRate))
        r.write(i16(channels * bits / 8)); r.write(i16(bits))
        r.write("data".toByteArray()); r.write(i32(dataLen))
    }

    private fun peak(buf: ByteArray, n: Int): Float {
        var maxV = 0
        var i = 0
        while (i < n - 1) {
            val s = (buf[i].toInt() and 0xff) or (buf[i + 1].toInt() shl 8)
            val v = abs(s)
            if (v > maxV) maxV = v
            i += 2
        }
        return (maxV / 32768f).coerceIn(0f, 1f)
    }

    private fun i32(v: Long) = byteArrayOf(
        (v and 0xff).toByte(), ((v shr 8) and 0xff).toByte(),
        ((v shr 16) and 0xff).toByte(), ((v shr 24) and 0xff).toByte()
    )

    private fun i16(v: Int) = byteArrayOf((v and 0xff).toByte(), ((v shr 8) and 0xff).toByte())
}
