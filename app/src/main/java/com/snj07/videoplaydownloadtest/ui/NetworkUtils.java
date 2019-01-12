package com.snj07.videoplaydownloadtest.ui;

import android.content.Context;
import android.net.ConnectivityManager;

public class NetworkUtils {

    public static Boolean isOnline(Context mContext) {

        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnected();
    }
}
