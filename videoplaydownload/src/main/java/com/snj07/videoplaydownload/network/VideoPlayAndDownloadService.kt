package com.snj07.videoplaydownload.network

import android.app.Activity
import java.io.File

class VideoPlayAndDownloadService(server: VideoStreamServer?){

    var server: VideoStreamServer? =  server


    constructor() :this(null){

    }

    fun startServer(
        activity: Activity,
        videoUrl: String,
        pathToSaveVideo: String,
        ipOfServer: String,
        callback: VideoStreamInterface
    ): VideoPlayAndDownloadService{

        VideoDownloader(videoUrl, pathToSaveVideo).startDownload()
        server = VideoStreamServer(File(pathToSaveVideo))
        server?.setSupportPlayWhileDownloading(true)
        Thread(Runnable {
            server?.init(ipOfServer)

            activity.runOnUiThread {
                server?.start()
                callback.onServerStart(server?.getFileUrl()!!)
            }
        }).start()

        return VideoPlayAndDownloadService(server)
    }

    fun isServerRunning(): Boolean {
        return server?.isRunning()!!
    }

    fun start() {
        server?.start()
    }

    fun stop() {
        server?.stop()
    }

    interface VideoStreamInterface {
        fun onServerStart(videoStreamUrl: String)

    }
}