package com.yago.grabadora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private val rates = arrayOf(44100, 48000)
    private val bitrates = arrayOf(128000, 192000, 256000, 320000)
    private lateinit var folderText: TextView
    private lateinit var bitrateSpinner: Spinner

    private val pickFolder = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Prefs.setTree(this, uri)
            updateFolder()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val formatSpinner = findViewById<Spinner>(R.id.formatSpinner)
        val rateSpinner = findViewById<Spinner>(R.id.rateSpinner)
        bitrateSpinner = findViewById(R.id.bitrateSpinner)
        folderText = findViewById(R.id.folderText)

        formatSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("WAV (sin pérdida)", "AAC / m4a (comprimido)")
        )
        rateSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, rates.map { "$it Hz" }
        )
        bitrateSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, bitrates.map { "${it / 1000} kbps" }
        )

        formatSpinner.setSelection(Prefs.format(this))
        rateSpinner.setSelection(rates.indexOf(Prefs.rate(this)).coerceAtLeast(0))
        bitrateSpinner.setSelection(bitrates.indexOf(Prefs.bitrate(this)).coerceAtLeast(0))
        bitrateSpinner.isEnabled = !Prefs.formatIsWav(this)

        formatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.setFormat(this@SettingsActivity, pos)
                bitrateSpinner.isEnabled = pos == 1
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        rateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.setRate(this@SettingsActivity, rates[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        bitrateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                Prefs.setBitrate(this@SettingsActivity, bitrates[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.folderBtn).setOnClickListener { pickFolder.launch(null) }
        updateFolder()
    }

    private fun updateFolder() {
        val name = Prefs.tree(this)?.let {
            DocumentFile.fromTreeUri(this, it)?.name ?: it.lastPathSegment
        } ?: "(ninguna)"
        folderText.text = "Carpeta: $name"
    }
}
