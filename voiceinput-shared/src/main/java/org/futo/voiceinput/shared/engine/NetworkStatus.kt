package org.futo.voiceinput.shared.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Provides the connectivity information needed to choose between online and on-device recognition. */
interface NetworkStatusProvider {
    /** True when the device currently has a validated internet connection. */
    fun hasInternetConnection(): Boolean
}

class ConnectivityManagerNetworkStatus(private val context: Context) : NetworkStatusProvider {
    override fun hasInternetConnection(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

/** Whether online speech recognition can be used right now (connected and service present). */
fun isOnlineRecognitionUsable(context: Context, networkStatus: NetworkStatusProvider): Boolean {
    return networkStatus.hasInternetConnection() && isSpeechRecognitionAvailable(context)
}
