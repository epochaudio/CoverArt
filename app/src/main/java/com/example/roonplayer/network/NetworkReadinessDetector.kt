package com.example.roonplayer.network

import android.content.Context
import android.net.*
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

class NetworkReadinessDetector(private val context: Context) {
    companion object {
        private const val TAG = "NetworkReadinessDetector"
        private const val CONNECTIVITY_CHECK_TIMEOUT = 3000
        private const val DNS_TEST_HOST = "8.8.8.8"
        private const val DNS_TEST_PORT = 53
    }

    sealed class NetworkState {
        object NotAvailable : NetworkState()
        object Connecting : NetworkState()
        object Available : NetworkState()
        data class Error(val message: String) : NetworkState()
    }

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    suspend fun waitForNetworkReady(timeoutMs: Long = 30000): NetworkState = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始检测网络就绪状态")

        val currentState = getCurrentNetworkState()
        if (currentState is NetworkState.Available) {
            Log.i(TAG, "网络已就绪")
            return@withContext currentState
        }

        return@withContext try {
            withTimeout(timeoutMs) {
                while (true) {
                    val state = getCurrentNetworkState()
                    when (state) {
                        is NetworkState.Available -> {
                            Log.i(TAG, "网络检测完成，状态就绪")
                            return@withTimeout state
                        }
                        is NetworkState.NotAvailable -> {
                            Log.d(TAG, "网络不可用，继续等待...")
                        }
                        is NetworkState.Connecting -> {
                            Log.d(TAG, "网络连接中，继续等待...")
                        }
                        is NetworkState.Error -> {
                            Log.w(TAG, "网络检测错误: ${state.message}")
                        }
                    }
                    delay(1000)
                }
                @Suppress("UNREACHABLE_CODE")
                NetworkState.Error("检测循环异常退出")
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "网络就绪检测超时")
            NetworkState.Error("网络检测超时，请检查网络连接")
        }
    }

    private suspend fun getCurrentNetworkState(): NetworkState = withContext(Dispatchers.IO) {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                return@withContext NetworkState.NotAvailable
            }

            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities == null) {
                return@withContext NetworkState.NotAvailable
            }

            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return@withContext NetworkState.Connecting
            }

            if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                if (isConnectivityTestPassed()) {
                    return@withContext NetworkState.Available
                }
                return@withContext NetworkState.Connecting
            }

            return@withContext NetworkState.Available

        } catch (e: Exception) {
            Log.e(TAG, "获取网络状态时发生错误", e)
            return@withContext NetworkState.Error("网络状态检测失败: ${e.message}")
        }
    }

    private suspend fun isConnectivityTestPassed(): Boolean = withContext(Dispatchers.IO) {
        try {
            withTimeout(CONNECTIVITY_CHECK_TIMEOUT.toLong()) {
                val socket = Socket()
                socket.connect(InetSocketAddress(DNS_TEST_HOST, DNS_TEST_PORT), CONNECTIVITY_CHECK_TIMEOUT)
                socket.close()
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "连通性测试失败: ${e.message}")
            false
        }
    }

    fun registerNetworkCallback(onNetworkChange: (NetworkState) -> Unit) {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "网络变为可用")
                CoroutineScope(Dispatchers.IO).launch {
                    val state = getCurrentNetworkState()
                    onNetworkChange(state)
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "网络连接丢失")
                onNetworkChange(NetworkState.NotAvailable)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                Log.d(TAG, "网络能力发生变化")
                CoroutineScope(Dispatchers.IO).launch {
                    val state = getCurrentNetworkState()
                    onNetworkChange(state)
                }
            }
        }

        connectivityManager.registerNetworkCallback(request, networkCallback!!)
    }

    fun unregisterNetworkCallback() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }
}