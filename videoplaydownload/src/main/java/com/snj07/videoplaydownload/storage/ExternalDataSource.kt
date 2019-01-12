package com.snj07.videoplaydownload.storage

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

class ExternalDataSource(resource: File){
    private var vFileResource: File =resource
    internal var contentLength: Long = 0
    private var inputStream: FileInputStream? = null
    private val TAG = ExternalDataSource::class.java.simpleName

    /**
     * Returns a MIME-compatible content type
     */
    fun getContentType(): String {
        return "video/mp4"
    }

    /**
     * Creates and opens an input stream that returns the contents of the resource.
     */
    fun createInputStream(): InputStream? {
        getInputStream()
        return inputStream
    }

    /**
     * Returns the length of resource in bytes.
     * else return -1 => for unknown size stream
     */
    fun getContentLength(ignoreSimulation: Boolean): Long {
        return if (!ignoreSimulation) {
            -1
        } else contentLength
    }

    fun getInputStream() {
        try {
            inputStream = FileInputStream(vFileResource)
            Log.e(TAG, "find found : " + vFileResource.exists())
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Cannot find file : " + vFileResource.exists())
            e.printStackTrace()
        }

        contentLength = vFileResource.length()
        Log.i(TAG, "content length is: $contentLength")
    }
}