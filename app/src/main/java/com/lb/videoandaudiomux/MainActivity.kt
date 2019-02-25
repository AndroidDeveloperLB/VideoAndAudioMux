package com.lb.videoandaudiomux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.RawRes
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.lb.videoandaudiomux.library.VideoAndAudioMuxer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: ScoreViewModel
    private var outputFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel = ViewModelProviders.of(this).get(ScoreViewModel::class.java)
        viewModel.muxState.observe(this,
            Observer<ScoreViewModel.MuxState> { state ->
                when (state) {
                    is ScoreViewModel.MuxState.IDLE -> {
                        viewSwitcher.setViewToSwitchTo(buttonsLayout)
                        outputFile = state.outputFile
                        openOutputFile.isEnabled = outputFile != null
                    }
                    is ScoreViewModel.MuxState.MUXING -> viewSwitcher.setViewToSwitchTo(progressBar)
                }
            })
        //sample files from:
        // long audio https://helpguide.sony.net/high-res/sample1/v1/en/index.html
        // short audio and video http://techslides.com/sample-files-for-development http://techslides.com/sample-files-for-development
        // long video https://standaloneinstaller.com/blog/big-list-of-sample-videos-for-testers-124.html
        muxShortVideoWithShortAudio.setOnClickListener {
            viewModel.muxVideoAndAudio(this@MainActivity, R.raw.video_5_sec, R.raw.audio_4_sec)
        }
        muxShortVideoWithLongAudio.setOnClickListener {
            viewModel.muxVideoAndAudio(this@MainActivity, R.raw.video_5_sec, R.raw.audio_40_sec)
        }
        muxLongVideoWithShortAudio.setOnClickListener {
            viewModel.muxVideoAndAudio(this@MainActivity, R.raw.video_38_sec, R.raw.audio_4_sec)
        }
        muxLongVideoWithLongAudio.setOnClickListener {
            viewModel.muxVideoAndAudio(this@MainActivity, R.raw.video_38_sec, R.raw.audio_40_sec)
        }

        openOutputFile.setOnClickListener {
            if (outputFile == null)
                return@setOnClickListener
            val filePath = outputFile!!.absolutePath
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(filePath))
            intent.setDataAndType(Uri.parse(filePath), "video/mp4")
            startActivity(intent)
        }
    }

    class ScoreViewModel : ViewModel() {
        val muxState = MutableLiveData<MuxState>()

        sealed class MuxState {
            class IDLE(val outputFile: File? = null) : MuxState()
            object MUXING : MuxState()
        }

        init {
//            if (muxState.value == null)
            muxState.value = MuxState.IDLE()
        }

        @UiThread
        fun muxVideoAndAudio(someContext: Context, @RawRes videoRawResId: Int, @RawRes audioRawResId: Int) {
            if (muxState.value is MuxState.MUXING)
                return
            val context = someContext.applicationContext
            muxState.value = MuxState.MUXING
            object : Thread() {
                override fun run() {
                    super.run()
                    Log.d("AppLog", "preparing files...")
                    val parentFile = context.getExternalFilesDir(null)!!
                    parentFile.mkdirs()
                    val videoFile = File(parentFile, "videoFile$videoRawResId.vid")
                    val audioFile = File(parentFile, "audioFile$audioRawResId.aud")
                    if (videoFile.exists()) {
                        val rawFileSize = context.resources.openRawResourceFd(videoRawResId).length
                        if (videoFile.length() != rawFileSize)
                            videoFile.delete()
                    }
                    if (!videoFile.exists()) {
                        context.resources.openRawResource(videoRawResId).toFile(videoFile)
                    }
                    if (audioFile.exists()) {
                        val rawFileSize = context.resources.openRawResourceFd(audioRawResId).length
                        if (audioFile.length() != rawFileSize)
                            audioFile.delete()
                    }
                    if (!audioFile.exists()) {
                        context.resources.openRawResource(audioRawResId).toFile(audioFile)
                    }
                    val newOutputFile = File(parentFile, "outputFile.vid")
                    Log.d("AppLog", "muxing... output: "+newOutputFile.absoluteFile)
                    val success = VideoAndAudioMuxer.joinVideoAndAudio(videoFile, audioFile, newOutputFile)
                    Log.d("AppLog", "done muxing. success?$success")
                    muxState.postValue(MuxState.IDLE(if (success) newOutputFile else null))
                }
            }.start()
        }


    }
}
