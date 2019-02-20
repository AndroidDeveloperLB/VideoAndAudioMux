package com.lb.videoandaudiomux.library

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaMuxer
import androidx.annotation.WorkerThread
import java.io.File
import java.nio.ByteBuffer

object VideoAndAudioMuxer {
    //   based on:  https://stackoverflow.com/a/31591485/878126
    @WorkerThread
    fun joinVideoAndAudio(videoFile: File, audioFile: File, outputFile: File): Boolean {
        try {
            //            val videoMediaMetadataRetriever = MediaMetadataRetriever()
            //            videoMediaMetadataRetriever.setDataSource(videoFile.absolutePath)
            //            val videoDurationInMs =
            //                videoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            //            val videoMimeType =
            //                videoMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            //            val audioMediaMetadataRetriever = MediaMetadataRetriever()
            //            audioMediaMetadataRetriever.setDataSource(audioFile.absolutePath)
            //            val audioDurationInMs =
            //                audioMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
            //            val audioMimeType =
            //                audioMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            //            Log.d(
            //                "AppLog",
            //                "videoDuration:$videoDurationInMs audioDuration:$audioDurationInMs videoMimeType:$videoMimeType audioMimeType:$audioMimeType"
            //            )
            //            videoMediaMetadataRetriever.release()
            //            audioMediaMetadataRetriever.release()
            outputFile.delete()
            outputFile.createNewFile()
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val sampleSize = 256 * 1024
            //video
            val videoExtractor = MediaExtractor()
            videoExtractor.setDataSource(videoFile.absolutePath)
            videoExtractor.selectTrack(0)
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val videoFormat = videoExtractor.getTrackFormat(0)
            val videoTrack = muxer.addTrack(videoFormat)
            val videoBuf = ByteBuffer.allocate(sampleSize)
            val videoBufferInfo = MediaCodec.BufferInfo()
//            Log.d("AppLog", "Video Format $videoFormat")
            //audio
            val audioExtractor = MediaExtractor()
            audioExtractor.setDataSource(audioFile.absolutePath)
            audioExtractor.selectTrack(0)
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val audioFormat = audioExtractor.getTrackFormat(0)
            val audioTrack = muxer.addTrack(audioFormat)
            val audioBuf = ByteBuffer.allocate(sampleSize)
            val audioBufferInfo = MediaCodec.BufferInfo()
//            Log.d("AppLog", "Audio Format $audioFormat")
            //
            muxer.start()
//            Log.d("AppLog", "muxing video&audio...")
            //            val minimalDurationInMs = Math.min(videoDurationInMs, audioDurationInMs)
            while (true) {
                videoBufferInfo.size = videoExtractor.readSampleData(videoBuf, 0)
                audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, 0)
                if (audioBufferInfo.size < 0) {
                    //                    Log.d("AppLog", "reached end of audio, looping...")
                    //TODO somehow start from beginning of the audio again, for looping till the video ends
                    //                    audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    //                    audioBufferInfo.size = audioExtractor.readSampleData(audioBuf, 0)
                }
                if (videoBufferInfo.size < 0 || audioBufferInfo.size < 0) {
//                    Log.d("AppLog", "reached end of video")
                    videoBufferInfo.size = 0
                    audioBufferInfo.size = 0
                    break
                } else {
                    //                    val donePercentage = videoExtractor.sampleTime / minimalDurationInMs / 10L
                    //                    Log.d("AppLog", "$donePercentage")
                    // video muxing
                    videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                    videoBufferInfo.flags = videoExtractor.sampleFlags
                    muxer.writeSampleData(videoTrack, videoBuf, videoBufferInfo)
                    videoExtractor.advance()
                    // audio muxing
                    audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                    audioBufferInfo.flags = audioExtractor.sampleFlags
                    muxer.writeSampleData(audioTrack, audioBuf, audioBufferInfo)
                    audioExtractor.advance()
                }
            }
            muxer.stop()
            muxer.release()
//            Log.d("AppLog", "success")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
//            Log.d("AppLog", "Error " + e.message)
        }
        return false
    }
}
