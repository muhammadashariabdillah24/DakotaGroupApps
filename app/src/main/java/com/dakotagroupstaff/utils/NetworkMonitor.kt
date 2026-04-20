package com.dakotagroupstaff.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NetworkMonitor — Real-time internet connectivity monitor.
 *
 * Uses [ConnectivityManager.NetworkCallback] to track when the device
 * gains or loses internet access. Exposes connectivity state as a [StateFlow]
 * so any coroutine-aware component (Activity, ViewModel) can observe changes
 * without polling.
 *
 * Lifecycle:
 * - [startMonitoring] must be called once (e.g., from Application.onCreate or BaseActivity.onStart)
 * - [stopMonitoring] releases the callback to prevent memory leaks
 *
 * This is registered as a Koin singleton so all components share the same instance.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Internal mutable state — starts with current connectivity status
    private val _isConnected = MutableStateFlow(checkCurrentConnectivity())

    /**
     * Observe this to react to connectivity changes.
     * true  = device has internet access
     * false = device has no internet access
     */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            Log.d("NetworkMonitor", "✅ Internet connection available")
            _isConnected.value = true
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            // Only emit true when the network actually has validated internet access
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            Log.d("NetworkMonitor", "Network capabilities changed — hasInternet=$hasInternet")
            _isConnected.value = hasInternet
        }

        override fun onLost(network: Network) {
            Log.w("NetworkMonitor", "❌ Internet connection lost")
            _isConnected.value = false
        }
    }

    /**
     * Start listening to network changes.
     * Safe to call multiple times — only registers once.
     */
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback)
            Log.d("NetworkMonitor", "Network monitoring started")
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Failed to register network callback", e)
        }
    }

    /**
     * Stop listening to network changes.
     * Call this in onStop/onDestroy to prevent memory leaks.
     */
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.d("NetworkMonitor", "Network monitoring stopped")
        } catch (e: Exception) {
            // Safe to ignore — callback may not have been registered
            Log.w("NetworkMonitor", "Network callback unregister failed (may not have been registered)")
        }
    }

    /**
     * Synchronous one-time check of current connectivity status.
     * Used to initialize [_isConnected] before the callback is registered.
     */
    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Current connectivity value (non-suspending convenience getter).
     */
    fun isCurrentlyConnected(): Boolean = _isConnected.value
}
