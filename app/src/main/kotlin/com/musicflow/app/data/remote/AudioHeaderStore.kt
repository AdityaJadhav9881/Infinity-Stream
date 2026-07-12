package com.musicflow.app.data.remote

import java.util.concurrent.ConcurrentHashMap

object AudioHeaderStore {

    private val headers = ConcurrentHashMap<String, Map<String, String>>()

    fun put(songId: String, httpHeaders: Map<String, String>) {
        headers[songId] = httpHeaders
    }

    fun get(songId: String): Map<String, String> = headers[songId] ?: emptyMap()

    fun remove(songId: String) {
        headers.remove(songId)
    }
}
