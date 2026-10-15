package com.swara.music.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Exposes whether the device currently has a usable internet connection.
 * Tracks every network that offers INTERNET, so losing Wi-Fi while mobile data is
 * still up (or switching on airplane mode) is reported correctly.
 */
class NetworkMonitor(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val networks = ConcurrentHashMap.newKeySet<Network>()
    private val _online = MutableStateFlow(initial())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { networks += network; publish() }
                override fun onLost(network: Network) { networks -= network; publish() }
                override fun onUnavailable() { publish() }
            })
        }
    }

    val isOnline: Boolean get() = _online.value

    private fun publish() { _online.value = networks.isNotEmpty() }

    private fun initial(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
