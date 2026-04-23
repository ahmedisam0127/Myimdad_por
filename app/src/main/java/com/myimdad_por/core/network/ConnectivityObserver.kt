package com.myimdad_por.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * مراقب حالة الاتصال بالإنترنت.
 *
 * يتتبع تغيّر الاتصال بشكل مباشر ليكون مناسبًا للـ UI و ViewModel.
 * يتطلب صلاحية:
 * android.permission.ACCESS_NETWORK_STATE
 */
interface ConnectivityObserver {

    /**
     * تدفق حالة الاتصال الحالية.
     */
    val status: Flow<Status>

    /**
     * هل الجهاز متصل الآن؟
     */
    fun isCurrentlyConnected(): Boolean

    /**
     * حالات الاتصال الممكنة.
     */
    enum class Status {
        Available,
        Unavailable,
        Losing,
        Lost
    }
}

/**
 * تنفيذ افتراضي لمراقبة الاتصال.
 */
class NetworkConnectivityObserver(
    context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val status: Flow<ConnectivityObserver.Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                trySend(ConnectivityObserver.Status.Available)
            }

            override fun onUnavailable() {
                trySend(ConnectivityObserver.Status.Unavailable)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(ConnectivityObserver.Status.Losing)
            }

            override fun onLost(network: Network) {
                trySend(ConnectivityObserver.Status.Lost)
            }
        }

        trySend(currentStatus())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }.distinctUntilChanged()

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    override fun isCurrentlyConnected(): Boolean {
        return currentStatus() == ConnectivityObserver.Status.Available
    }

    private fun currentStatus(): ConnectivityObserver.Status {
        val activeNetwork = connectivityManager.activeNetwork ?: return ConnectivityObserver.Status.Unavailable
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return ConnectivityObserver.Status.Unavailable

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        return if (hasInternet && isValidated) {
            ConnectivityObserver.Status.Available
        } else {
            ConnectivityObserver.Status.Unavailable
        }
    }
}