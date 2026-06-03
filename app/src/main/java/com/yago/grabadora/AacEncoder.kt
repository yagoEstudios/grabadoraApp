package com.yago.grabadora

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.FileInputStream

/** Codifica un WAV PCM mono a un .m4a (AAC-LC) usando MediaCodec + MediaMuxer. */
object AacEncoder {

    fun encode(pcmWav: File, outM4a: File, sampleRate: Int, bitRate: Int) {
        val channels = 1
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
        )
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outM4a.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val input = FileInputStream(pcmWav)
        input.skip(44) // saltar cabecera WAV
        val info = MediaCodec.BufferInfo()
        val readBuf = ByteArray(16384)
        val bytesPerSec = sampleRate * channels * 2
        var presentationUs = 0L
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        inBuf.clear()
                        val toRead = minOf(inBuf.capacity(), readBuf.size)
                        val read = input.read(readBuf, 0, toRead)
                        if (read < 0) {
                            codec.queueInputBuffer(
                                inIndex, 0, 0, presentationUs,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            inBuf.put(readBuf, 0, read)
                            codec.queueInputBuffer(inIndex, 0, read, presentationUs, 0)
                            presentationUs += read.toLong() * 1_000_000L / bytesPerSec
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (outIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)!!
                    if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                    if (info.size > 0 && muxerStarted) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        muxer.writeSampleData(trackIndex, outBuf, info)
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                }
            }
        } finally {
            try { input.close() } catch (_: Exception) {}
            try { codec.stop() } catch (_: Exception) {}
            codec.release()
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
    }
}
