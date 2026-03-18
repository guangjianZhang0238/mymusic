package com.music.app.data.remote

import android.content.Context
import android.net.Uri

object ServerEndpointStore {
    private const val PREFS_NAME = "music_app_prefs"
    private const val KEY_SERVER_ENDPOINT = "server_endpoint_v1"

    enum class ServerEndpoint(val id: Int, val displayName: String) {
        SERVER_1(1, "服务器1"),
        SERVER_2(2, "服务器2")
    }

    fun getSelected(context: Context): ServerEndpoint {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getInt(KEY_SERVER_ENDPOINT, ServerEndpoint.SERVER_1.id)) {
            ServerEndpoint.SERVER_2.id -> ServerEndpoint.SERVER_2
            else -> ServerEndpoint.SERVER_1
        }
    }

    fun setSelected(context: Context, endpoint: ServerEndpoint) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SERVER_ENDPOINT, endpoint.id).apply()
    }

    fun getSelectedBaseUrl(context: Context): String {
        val endpoint = getSelected(context)
        return when (endpoint) {
            ServerEndpoint.SERVER_1 -> NetworkModule.BASE_URL
            ServerEndpoint.SERVER_2 -> NetworkModule.BASE_URL_2
        }
    }

    fun getSelectedBaseUri(context: Context): Uri {
        return Uri.parse(getSelectedBaseUrl(context))
    }
}
