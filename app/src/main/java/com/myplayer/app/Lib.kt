package com.myplayer.app

import android.content.Context

object Lib {
    private fun p(c: Context) = c.getSharedPreferences("lib", Context.MODE_PRIVATE)
    private fun split(s: String?): List<String> = (s ?: "").split("\n").filter { it.isNotBlank() }
    private fun put(c: Context, k: String, l: List<String>) { p(c).edit().putString(k, l.joinToString("\n")).apply() }

    fun recent(c: Context) = split(p(c).getString("recent", ""))
    fun addRecent(c: Context, u: String) {
        val l = recent(c).toMutableList()
        l.remove(u); l.add(0, u)
        put(c, "recent", l.take(60))
    }
    fun playlists(c: Context): List<String> {
        val l = split(p(c).getString("pls", "")).toMutableList()
        if (!l.contains("Favorites")) l.add(0, "Favorites")
        return l
    }
    fun items(c: Context, name: String) = split(p(c).getString("p:$name", ""))
    fun create(c: Context, name: String) {
        val l = playlists(c).toMutableList()
        if (!l.contains(name)) l.add(name)
        put(c, "pls", l)
    }
    fun add(c: Context, name: String, u: String) {
        create(c, name)
        val l = items(c, name).toMutableList()
        if (!l.contains(u)) l.add(u)
        put(c, "p:$name", l)
    }
    fun remove(c: Context, name: String, u: String) {
        val l = items(c, name).toMutableList()
        l.remove(u)
        put(c, "p:$name", l)
    }
    fun deletePlaylist(c: Context, name: String) {
        val l = playlists(c).toMutableList()
        l.remove(name)
        put(c, "pls", l)
        p(c).edit().remove("p:$name").apply()
    }
    fun on(c: Context, k: String) = p(c).getBoolean("s_$k", true)
    fun setOn(c: Context, k: String, v: Boolean) { p(c).edit().putBoolean("s_$k", v).apply() }
    fun bookmarks(c: Context, u: String): List<Long> = split(p(c).getString("b:$u", "")).mapNotNull { it.toLongOrNull() }.sorted()
    fun addBookmark(c: Context, u: String, t: Long) {
        val l = bookmarks(c, u).toMutableList()
        if (!l.contains(t)) l.add(t)
        put(c, "b:$u", l.map { it.toString() })
    }
    fun clearBookmarks(c: Context, u: String) { p(c).edit().remove("b:$u").apply() }
    fun streams(c: Context) = split(p(c).getString("streams", ""))
    fun addStream(c: Context, u: String) {
        val l = streams(c).toMutableList()
        l.remove(u); l.add(0, u)
        put(c, "streams", l.take(30))
    }
}
