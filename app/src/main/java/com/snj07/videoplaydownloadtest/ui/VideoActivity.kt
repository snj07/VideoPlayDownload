package com.snj07.videoplaydownloadtest.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import com.snj07.videoplaydownloadtest.R

import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import com.snj07.videoplaydownload.network.VideoPlayAndDownloadService
import com.snj07.videoplaydownloadtest.utils.Constants
import java.io.File

class VideoActivity : AppCompatActivity() {


    companion object {
        private val DELAY = 2000
        val TAG = VideoActivity::class.java.simpleName
    }

    private var mediaController: MediaController? = null
    private var videoView: VideoView? = null
    private var fileName: String? = null
    private var videoPath: Uri? = null
    private var videoService: VideoPlayAndDownloadService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_view_activity)

        videoView = findViewById(R.id.videoView)
        mediaController = MediaController(this)
        mediaController!!.setAnchorView(videoView)
        videoView!!.setMediaController(mediaController)


        fileName = Constants.VIDEO_URL.substring(Constants.VIDEO_URL.lastIndexOf('/') + 1)
        Log.i(TAG, "Video file name: " + fileName!!)
        val path = filesDir.toString() + "/" + fileName
        Log.i(TAG, "Path: $path")
        val file = File(path)
        Log.i(TAG, "File exists: " + file.exists())

        if (file.exists()) {
            videoPath = Uri.parse(path)
            videoView!!.setVideoURI(videoPath)
            videoView!!.requestFocus()
            videoView!!.start()

        } else {
            if (NetworkUtils.isOnline(applicationContext)) {
                startServer(path)
            } else {
                Toast.makeText(this@VideoActivity, resources.getText(R.string.no_internet), Toast.LENGTH_LONG).show()
                Handler().postDelayed({ finish() }, VideoActivity.DELAY.toLong())
            }

        }


    }

    fun startServer(path: String) {
        videoService = VideoPlayAndDownloadService().startServer(
            this,
            Constants.VIDEO_URL,
            path,
            Constants.LOCAL_IP,
            object : VideoPlayAndDownloadService.VideoStreamInterface {
                override fun onServerStart(videoStreamUrl: String) {
                    videoPath = Uri.parse(videoStreamUrl)
                    videoView!!.setVideoURI(videoPath)
                    videoView!!.requestFocus()
                    videoView!!.start()

                }
            })
    }



    override fun onStop() {
        super.onStop()
        if (videoService != null) {
            videoService!!.stop()
        }

    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

}

