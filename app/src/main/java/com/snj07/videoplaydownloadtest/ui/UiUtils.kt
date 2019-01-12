package com.snj07.videoplaydownloadtest.ui

import android.app.Activity
import android.support.design.widget.Snackbar
import android.view.View
import com.snj07.videoplaydownloadtest.R

class UiUtils {
  companion object {
      fun showSnackbar(activity: Activity, msg: String) {
          val parentLayout = activity.findViewById<View>(android.R.id.content)
          Snackbar.make(parentLayout, msg, Snackbar.LENGTH_LONG)
              .setAction(activity.resources.getText(R.string.close), View.OnClickListener {
                  //do nothing
              })
              .setActionTextColor(activity.resources.getColor(android.R.color.holo_red_light))
              .show()
      }
  }
}