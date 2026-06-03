package com.yago.grabadora

import android.content.Context
import android.net.Uri

object Prefs {
    private const val P = "grab_prefs"
    private const val K_FORMAT = "format" // 0 = WAV, 1 = AAC
    private const val K_RATE = "rate"
    private const val K_BITRATE = "bitrate"
    private const val K_TREE = "tree_uri"

    private fun sp(c: Context) = c.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun format(c: Context) = sp(c).getInt(K_FORMAT, 0)
    fun formatIsWav(c: Context) = format(c) == 0
    fun setFormat(c: Context, v: Int) = sp(c).edit().putInt(K_FORMAT, v).apply()

    fun rate(c: Context) = sp(c).getInt(K_RATE, 44100)
    fun setRate(c: Context, v: Int) = sp(c).edit().putInt(K_RATE, v).apply()

    fun bitrate(c: Context) = sp(c).getInt(K_BITRATE, 256000)
    fun setBitrate(c: Context, v: Int) = sp(c).edit().putInt(K_BITRATE, v).apply()

    fun tree(c: Context): Uri? = sp(c).getString(K_TREE, null)?.let { Uri.parse(it) }
    fun setTree(c: Context, uri: Uri) = sp(c).edit().putString(K_TREE, uri.toString()).apply()
}
