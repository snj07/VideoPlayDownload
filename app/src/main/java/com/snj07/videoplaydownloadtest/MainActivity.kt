package com.snj07.videoplaydownloadtest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.snj07.videoplaydownloadtest.ui.UiUtils
import com.snj07.videoplaydownloadtest.ui.VideoActivity
import com.snj07.videoplaydownloadtest.utils.Constants
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    val TAG = VideoActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start_button.setOnClickListener(View.OnClickListener {
            startActivity(
                Intent(
                    this@MainActivity,
                    VideoActivity::class.java
                )
            )
        })

        delete_button.setOnClickListener(View.OnClickListener {
            val fileName = Constants.VIDEO_URL.substring(Constants.VIDEO_URL.lastIndexOf('/') + 1)
            val path = filesDir.absolutePath + "/" + fileName
            val file = File(path)
            if (file.exists()) {
                Log.i(TAG, "video deleted")
                file.delete()
                applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                UiUtils.showSnackbar(this@MainActivity, "Video Deleted!!")

            } else {
                UiUtils.showSnackbar(this@MainActivity, "Video Not Available!!")
            }
        })
    }
}
