package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object NetworkMonitor {
    fun observeIsOnline(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val cm = try {
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        } catch (_: Exception) {
            null
        }

        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        fun checkNow(): Boolean {
            return try {
                val active = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(active) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (_: Exception) {
                false
            }
        }

        trySend(checkNow())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkNow())
            }

            override fun onLost(network: Network) {
                trySend(checkNow())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                )
            }
        }

        val registered = try {
            cm.registerDefaultNetworkCallback(callback)
            true
        } catch (_: Exception) {
            false
        }

        awaitClose {
            if (registered) {
                try {
                    cm.unregisterNetworkCallback(callback)
                } catch (_: Exception) {
                    // Ignore cleanup failures
                }
            }
        }
    }.distinctUntilChanged()
}
