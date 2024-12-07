package com.ovisionik.memotag.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object AppNetwork {

    /**
     *A Simple function to check if internet is available
    **/
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}