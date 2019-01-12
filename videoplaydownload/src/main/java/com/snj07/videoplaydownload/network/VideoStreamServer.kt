package com.snj07.videoplaydownload.network

import android.util.Log
import com.snj07.videoplaydownload.storage.ExternalDataSource
import java.io.*
import java.net.*
import java.util.*

class VideoStreamServer(file: File) : Runnable {
    private val TAG = VideoStreamServer::class.java!!.getName()
    private var port = 0
    private var isRunning = false
    private var socket: ServerSocket? = null
    private var thread: Thread? = null
    private var cbSkip: Long = 0
    private var seekRequest: Boolean = false
    private val vFile: File = file


    private var supportPlayWhileDownloading = false


    fun getPort(): Int {
        return port
    }


    /**
     * Prepare the server to start.
     * This only needs to be called once per instance. Once initialized, the
     * server can be started and stopped as needed.
     */
    fun init(ip: String): String? {
        var url: String? = null
        try {
            val inet = InetAddress.getByName(ip)

            val bytes = inet.address
            socket = ServerSocket(port, 0, InetAddress.getByAddress(bytes))
            socket?.setSoTimeout(0)
            port = socket?.getLocalPort()!!
            url = ("http://" + socket?.getInetAddress()?.hostAddress + ":"
                    + port)
            Log.e(TAG, "Server started at $url")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Error UnknownHostException server", e)
        } catch (e: IOException) {
            Log.e(TAG, "Error IOException server", e)
        }

        return url
    }

    fun getFileUrl(): String {
        return ("http://" + socket?.getInetAddress()?.hostAddress + ":"
                + port + "/" + vFile.name)
    }

    /**
     * Start the server.
     */
    fun start() {
        thread = Thread(this)
        thread?.setPriority(Thread.MAX_PRIORITY)
        thread?.start()
        isRunning = true
    }

    /**
     * Stop stops the thread listening to the port. It may take up to five
     * seconds to close the service and this call blocks until that occurs.
     */
    fun stop() {
        isRunning = false
        if (thread == null) {
            Log.e(TAG, "Server was stopped without being started.")
            return
        }
        Log.e(TAG, "Stopping server.")
        thread?.interrupt()
    }

    /**
     * Checks if the server is running
     */
    fun isRunning(): Boolean {
        return isRunning
    }

    /**
     * This is used internally by the server and should not be called directly.
     */
    override fun run() {
        Log.e(TAG, "running")
        while (isRunning) {
            try {
                val client = socket?.accept() ?: continue
                Log.e(TAG, "client connected at $port")
                val data = ExternalDataSource(
                    vFile
                )
                Log.e(TAG, "processing request...")
                processRequest(data, client)
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "No client connected, waiting for client...", e)
            } catch (e: IOException) {
                Log.e(TAG, "Error connecting to client", e)
            }

        }
        Log.e(TAG, "Server interrupted or stopped. Shutting down.")
    }

    /**
     * Find byte index separating header from body.
     * It must be the last byte of the first two sequential new lines.
     */
    private fun findHeaderEnd(buf: ByteArray, rLen: Int): Int {
        var splitByte = 0
        while (splitByte + 3 < rLen) {
            if (buf[splitByte] == '\r'.toByte() && buf[splitByte + 1] == '\n'.toByte()
                && buf[splitByte + 2] == '\r'.toByte() && buf[splitByte + 3] == '\n'.toByte()
            )
                return splitByte + 4
            splitByte++
        }
        return 0
    }

    /*
     * Sends the HTTP response to the client, including headers (as applicable)
     * and content.
     */
    @Throws(IllegalStateException::class, IOException::class)
    private fun processRequest(
        dataSource: ExternalDataSource?,
        client: Socket?
    ) {
        if (dataSource == null) {
            Log.e(TAG, "Invalid (null) resource.")
            client!!.close()
            return
        }
        val `is` = client!!.getInputStream()
        val buffSize = 8192
        val buf = ByteArray(buffSize)
        var splitByte: Int
        var rLen = 0
        run {
            var read = `is`.read(buf, 0, buffSize)
            while (read > 0) {
                rLen += read
                splitByte = findHeaderEnd(buf, rLen)
                if (splitByte > 0)
                    break
                read = `is`.read(buf, rLen, buffSize - rLen)
            }
        }

        // Create a BufferedReader for parsing the header.
        val byteArrayInputStream = ByteArrayInputStream(buf, 0, rLen)
        val hin = BufferedReader(InputStreamReader(byteArrayInputStream))
        val pre = Properties()
        val params = Properties()
        val header = Properties()

        try {
            decodeHeader(hin, pre, params, header)
        } catch (e1: InterruptedException) {
            Log.e(TAG, "Exception: " + e1.message)
            e1.printStackTrace()
        }

        for ((key, value) in header) {
            Log.e(TAG, "Header: $key : $value")
        }
        var range: String? = header.getProperty("range")
        cbSkip = 0
        seekRequest = false
        if (range != null) {
            Log.i(TAG, "range is: $range")
            seekRequest = true
            range = range.substring(6)
            val charPos = range.indexOf('-')
            if (charPos > 0) {
                range = range.substring(0, charPos)
            }
            cbSkip = java.lang.Long.parseLong(range)
            Log.e(TAG, "range found!! $cbSkip")
        }
        var headers = ""
        // Log.e(TAG, "is seek request: " + seekRequest);
        if (seekRequest) {// It is a seek or skip request if there's a Range
            // header
            headers += "HTTP/1.1 206 Partial Content\r\n"
            headers += "Content-Type: " + dataSource!!.getContentType() + "\r\n"
            headers += "Accept-Ranges: bytes\r\n"
            headers += ("Content-Length: " + dataSource!!.getContentLength(false)
                    + "\r\n")
            headers += ("Content-Range: bytes " + cbSkip + "-"
                    + dataSource!!.getContentLength(true) + "/*\r\n")
            headers += "\r\n"
        } else {
            headers += "HTTP/1.1 200 OK\r\n"
            headers += "Content-Type: " + dataSource!!.getContentType() + "\r\n"
            headers += "Accept-Ranges: bytes\r\n"
            headers += ("Content-Length: " + dataSource!!.getContentLength(false)
                    + "\r\n")
            headers += "\r\n"
        }

        var data: InputStream? = null
        try {
            data = dataSource!!.createInputStream()
            val buffer = headers.toByteArray()
            Log.e(TAG, "writing to client")
            client.getOutputStream().write(buffer, 0, buffer.size)

            // Start sending content.

            val buff = ByteArray(1024 * 50)
            Log.e(TAG, "No of bytes skipped: " + data!!.skip(cbSkip))
            var cbSentThisBatch = 0
            while (isRunning) {
                if (supportPlayWhileDownloading) {
                    // Check if data is ready
                    while (!VideoDownloader.isDataReady() && isRunning) {
                        if (VideoDownloader.dataStatus === VideoDownloader.DATA_READY) {
                            Log.i(TAG, "start reading bytes : state : Data ready")
                            break
                        } else if (VideoDownloader.dataStatus === VideoDownloader.DATA_CONSUMED) {
                            Log.i(TAG, "reading bytes end : state : All Data consumed")
                            break
                        } else if (VideoDownloader.dataStatus === VideoDownloader.DATA_NOT_READY) {
                            Log.e(TAG, "error in reading bytes : state : Data not ready")
                        } else if (VideoDownloader.dataStatus === VideoDownloader.DATA_NOT_AVAILABLE) {
                            Log.e(TAG, "error in reading bytes : state : Data not available")
                        }
                    }
                    Log.i(TAG, "reading bytes : Data ready")
                }

                var cbRead = data!!.read(buff, 0, buff.size)
                if (cbRead == -1) {
                    Log.e(TAG, "ready bytes are -1 and this is simulate streaming, close the ips and create another  ")
                    data.close()
                    data = dataSource!!.createInputStream()
                    cbRead = data!!.read(buff, 0, buff.size)
                    if (cbRead == -1) {
                        Log.e(TAG, "error in reading bytes")
                        //                        throw new IOException(
                        //                                "Error re-opening data source for looping.");
                        break
                    }
                }
                client.getOutputStream().write(buff, 0, cbRead)
                client.getOutputStream().flush()
                cbSkip += cbRead.toLong()
                cbSentThisBatch += cbRead

                if (supportPlayWhileDownloading)
                    VideoDownloader.consumedBytes += cbRead
            }
            Log.e(TAG, "cbSentThisBatch: $cbSentThisBatch")
            // If we did nothing this batch, block for a second
            if (cbSentThisBatch == 0) {
                Log.e(TAG, "Blocking until more data appears")
                Thread.sleep(1000)
            }
        } catch (e: SocketException) {
            // Ignore when the client breaks connection
            Log.e(TAG, "Ignoring " + e.message)
        } catch (e: IOException) {
            Log.e(TAG, "Error getting content stream.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming file content.", e)
        } finally {
            data?.close()
            client?.close()
        }
    }


    /**
     * Decodes the sent headers and loads the data into java Properties' key -
     * value pairs
     */
    @Throws(InterruptedException::class)
    private fun decodeHeader(
        `in`: BufferedReader, pre: Properties,
        params: Properties, header: Properties
    ) {
        try {
            // Read the request line
            val inLine = `in`.readLine() ?: return
            val st = StringTokenizer(inLine)
            if (!st.hasMoreTokens())
                Log.e(TAG, "BAD REQUEST: Syntax error. Usage: GET /example/file.html")

            val method = st.nextToken()
            pre["method"] = method

            if (!st.hasMoreTokens())
                Log.e(TAG, "BAD REQUEST: Missing URI. Usage: GET /example/file.html")

            var uri: String? = st.nextToken()

            // Decode parameters from the URI
            val qmi = uri!!.indexOf('?')
            if (qmi >= 0) {
                decodeParms(uri.substring(qmi + 1), params)
                uri = decodePercent(uri.substring(0, qmi))
            } else
                uri = decodePercent(uri)

            // If there's another token, it's protocol version,
            // followed by HTTP headers. Ignore version but parse headers.
            // NOTE: this now forces header names lowercase since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                var line: String? = `in`.readLine()
                while (line != null && line.trim { it <= ' ' }.length > 0) {
                    val p = line.indexOf(':')
                    if (p >= 0)
                        header[line.substring(0, p).trim { it <= ' ' }.toLowerCase()] =
                                line.substring(p + 1).trim { it <= ' ' }
                    line = `in`.readLine()
                }
            }

            pre["uri"] = uri
        } catch (ioe: IOException) {
            Log.e(
                TAG,
                "SERVER INTERNAL ERROR: IOException: " + ioe.message
            )
        }

    }

    /**
     * Decodes parameters in percent-encoded URI-format ( e.g.
     * "name=sanjay%20sharmas&pass=dontkillit" ) and adds them to given
     * Properties. NOTE: this doesn't support multiple identical keys due to the
     * simplicity of Properties -- if you need multiples, you might want to
     * replace the Properties with a Hashtable of Vectors or such.
     */
    @Throws(InterruptedException::class)
    private fun decodeParms(params: String?, p: Properties) {
        if (params == null)
            return

        val st = StringTokenizer(params, "&")
        while (st.hasMoreTokens()) {
            val e = st.nextToken()
            val sep = e.indexOf('=')
            if (sep >= 0)
                p[decodePercent(e.substring(0, sep))!!.trim { it <= ' ' }] = decodePercent(e.substring(sep + 1))!!
        }
    }

    /**
     * Decodes the percent encoding scheme.
     * For example: "an+example%20string" -> "an example string"
     */
    private fun decodePercent(str: String): String? {
        try {
            val sb = StringBuilder()
            var i = 0
            while (i < str.length) {
                val c = str[i]
                when (c) {
                    '+' -> sb.append(' ')
                    '%' -> {
                        sb.append(Integer.parseInt(str.substring(i + 1, i + 3), 16).toChar())
                        i += 2
                    }
                    else -> sb.append(c)
                }
                i++
            }
            return sb.toString()
        } catch (e: RuntimeException) {
            Log.e(TAG, "BAD REQUEST: bad request encoding.")
            return null
        }

    }

    fun isSupportPlayWhileDownloading(): Boolean {
        return supportPlayWhileDownloading
    }

    fun setSupportPlayWhileDownloading(supportPlayWhileDownloading: Boolean) {
        this.supportPlayWhileDownloading = supportPlayWhileDownloading
    }
}