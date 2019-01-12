package com.snj07.videoplaydownload.network

import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class VideoDownloader(videoUrl :String , path : String ) {


    private var path: String = path
    private var vUrl: String = videoUrl


    private var isFileDownloaded: Boolean = false


    companion object {
        const val RC_SIGN_IN: Int = 9001
        val DATA_READY = 1
        val DATA_NOT_READY = 2
        val DATA_CONSUMED = 3
        val DATA_NOT_AVAILABLE = 4
        //Keeps track of read bytes while serving to video player client from server
        var consumedBytes = 0
        var dataStatus = -1
        //Length of file being downloaded.
        var fileLength = -1L


        //Keeps track of all bytes read on each while iteration
        private var readBytes = 0L
        val TAG: String = "VideoDownloadee"
        fun isDataReady(): Boolean {
            dataStatus = -1
            var res = false
            if (fileLength == readBytes) {
                dataStatus = DATA_CONSUMED
                res = false
            } else if (readBytes > consumedBytes) {
                dataStatus = DATA_READY
                res = true
            } else if (readBytes <= consumedBytes) {
                dataStatus = DATA_NOT_READY
                res = false
            } else if (fileLength == -1L) {
                dataStatus = DATA_NOT_AVAILABLE
                res = false
            }
            return res
        }
        init {
            //here goes static initializer code

        }
    }

    fun startDownload() {
        Thread(Runnable {
            var input: BufferedInputStream? = null
            try {
                val out = FileOutputStream(path)

                try {
                    val url = URL(vUrl)

                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 0
                    connection.connect()
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        throw RuntimeException("response is not HTTP_OK")
                    }
                    fileLength = connection.contentLength.toLong()

                    input = BufferedInputStream(connection.inputStream)
                    val data = ByteArray(1024 * 50)

//                    var readBytes: Long = 0
                    var len = 0

                    while ((input!!.read(data).also { len = it }) != -1) {
                        out.write(data, 0, len)
                        out.flush()
                        readBytes += len.toLong()
                        readBytes += len
                        Log.w("download", (readBytes / 1024).toString() + "kb of " + fileLength / 1024 + "kb")
                    }
                    Log.w("download...", (readBytes / 1024).toString() + "kb /" + fileLength / 1024 + "kb")

                    setFileDownloaded(true)
                } catch (e: MalformedURLException) {
                    Log.e(TAG, "Error in URL : $path")
                } catch (e: IOException) {
                    Log.e(TAG, "IO Error  : " + e.localizedMessage)
                    //delete file if not downloaded completely
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } finally {
                    if (readBytes < fileLength) {
                        val file = File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    if (out != null) {
                        out.flush()
                        out.close()
                    }
                    input?.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }).start()
    }


    fun isFileDownloaded(): Boolean {
        return isFileDownloaded
    }

    fun setFileDownloaded(fileDownloaded: Boolean) {
        isFileDownloaded = fileDownloaded
    }
}