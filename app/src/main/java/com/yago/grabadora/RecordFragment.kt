package com.yago.grabadora

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class RecordFragment : Fragment() {

    private enum class State { IDLE, RECORDING, PAUSED }

    private var state = State.IDLE
    private var recorder: PcmRecorder? = null
    private var workFile: File? = null

    private lateinit var waveform: WaveformView
    private lateinit var timer: TextView
    private lateinit var mainBtn: ImageButton
    private lateinit var btnListen: ImageButton
    private lateinit var pausedPanel: LinearLayout
    private lateinit var playerBar: LinearLayout
    private lateinit var player: AudioPlayer

    private val askPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else toast("Permiso de micrófono denegado")
    }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        inflater.inflate(R.layout.fragment_record, c, false)

    override fun onViewCreated(view: View, s: Bundle?) {
        waveform = view.findViewById(R.id.waveform)
        timer = view.findViewById(R.id.timer)
        mainBtn = view.findViewById(R.id.mainBtn)
        pausedPanel = view.findViewById(R.id.pausedPanel)
        playerBar = view.findViewById(R.id.playerBar)
        btnListen = view.findViewById(R.id.btnListen)
        player = AudioPlayer(
            requireContext(),
            btnListen,
            view.findViewById(R.id.seek),
            view.findViewById(R.id.playTime),
            null
        )

        mainBtn.setOnClickListener { onMain() }
        view.findViewById<ImageButton>(R.id.btnDiscard).setOnClickListener { discard() }
        view.findViewById<ImageButton>(R.id.btnResume).setOnClickListener { resume() }
        btnListen.setOnClickListener { onListen() }
        view.findViewById<ImageButton>(R.id.btnSave).setOnClickListener { saveDialog() }
        applyState()
    }

    private fun onMain() {
        when (state) {
            State.IDLE -> ensurePermAndStart()
            State.RECORDING -> pause()
            State.PAUSED -> {}
        }
    }

    private fun ensurePermAndStart() {
        if (Prefs.tree(requireContext()) == null) {
            toast("Elige una carpeta en Ajustes"); return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            askPerm.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        player.release()
        playerBar.visibility = View.GONE
        waveform.clear()
        val rate = Prefs.rate(requireContext())
        val f = File(requireContext().cacheDir, "work.wav")
        if (f.exists()) f.delete()
        workFile = f
        val rec = PcmRecorder(rate, { waveform.push(it) }, { timer.text = fmt(it) })
        recorder = rec
        rec.start(f)
        state = State.RECORDING
        applyState()
    }

    private fun pause() {
        recorder?.pause()
        state = State.PAUSED
        applyState()
    }

    private fun resume() {
        player.release()
        btnListen.setImageResource(R.drawable.ic_play)
        playerBar.visibility = View.GONE
        recorder?.resume()
        state = State.RECORDING
        applyState()
    }

    private fun discard() {
        player.release()
        btnListen.setImageResource(R.drawable.ic_play)
        playerBar.visibility = View.GONE
        recorder?.discard()
        recorder = null
        workFile = null
        waveform.clear()
        timer.text = fmt(0)
        state = State.IDLE
        applyState()
    }

    private fun onListen() {
        val f = workFile ?: return
        if (!player.isLoaded) {
            playerBar.visibility = View.VISIBLE
            player.load(Uri.fromFile(f))
        } else {
            player.toggle()
        }
    }

    private fun saveDialog() {
        if (workFile == null) return
        val isWav = Prefs.formatIsWav(requireContext())
        val ext = if (isWav) ".wav" else ".m4a"
        val def = "REC_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val input = EditText(requireContext()).apply {
            setText(def)
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Guardar como")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text.toString().trim().ifBlank { def } + ext
                save(name, isWav)
            }
            .show()
    }

    private fun save(name: String, isWav: Boolean) {
        val ctx = requireContext().applicationContext
        player.release()
        val work = recorder?.finish() ?: workFile ?: return
        val treeUri = Prefs.tree(ctx) ?: run { toast("Carpeta no válida"); return }
        val tree = DocumentFile.fromTreeUri(ctx, treeUri) ?: run { toast("Carpeta no válida"); return }
        toast("Guardando…")
        thread {
            try {
                if (isWav) {
                    val doc = tree.createFile("audio/wav", name) ?: return@thread
                    ctx.contentResolver.openOutputStream(doc.uri)?.use { out ->
                        FileInputStream(work).use { it.copyTo(out) }
                    }
                } else {
                    val temp = File(ctx.cacheDir, "out.m4a")
                    if (temp.exists()) temp.delete()
                    AacEncoder.encode(work, temp, Prefs.rate(ctx), Prefs.bitrate(ctx))
                    val doc = tree.createFile("audio/mp4", name) ?: return@thread
                    ctx.contentResolver.openOutputStream(doc.uri)?.use { out ->
                        FileInputStream(temp).use { it.copyTo(out) }
                    }
                    temp.delete()
                }
                activity?.runOnUiThread { toast("Guardado"); discard() }
            } catch (e: Exception) {
                activity?.runOnUiThread { toast("Error al guardar: ${e.message}") }
            }
        }
    }

    private fun applyState() {
        when (state) {
            State.IDLE -> {
                mainBtn.visibility = View.VISIBLE
                mainBtn.setImageResource(R.drawable.ic_mic)
                mainBtn.backgroundTintList = color(R.color.record_red)
                pausedPanel.visibility = View.GONE
            }
            State.RECORDING -> {
                mainBtn.visibility = View.VISIBLE
                mainBtn.setImageResource(R.drawable.ic_stop)
                mainBtn.backgroundTintList = color(R.color.record_red)
                pausedPanel.visibility = View.GONE
                playerBar.visibility = View.GONE
            }
            State.PAUSED -> {
                mainBtn.visibility = View.GONE
                pausedPanel.visibility = View.VISIBLE
            }
        }
    }

    private fun color(res: Int) =
        ColorStateList.valueOf(ContextCompat.getColor(requireContext(), res))

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
        if (state == State.RECORDING) recorder?.pause()
    }
}
