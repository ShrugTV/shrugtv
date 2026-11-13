package com.shrug.shrugtv.apihelpers

import android.content.Context
import android.os.Looper
import com.shrug.shrugtv.api.helix.HelixApi
import com.shrug.shrugtv.api.helix.HelixParser
import com.shrug.shrugtv.model.Movie
import com.shrug.shrugtv.telemetry.CrashReporter
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves and caches channel avatars and logins by Helix user ID (replaces decompiled
 * ChannelAvatarLoader.java).
 */
class ChannelAvatarLoader {

    companion object {
        private val CACHE = ConcurrentHashMap<Int, Pair<String, String>>()

        fun cacheUsers(users: List<Movie>?) {
            if (users == null) return
            for (u in users) {
                val uid = u.userId
                val avatar = u.avatarUrl
                if (uid > 0 && !avatar.isNullOrEmpty()) {
                    CACHE[uid] = avatar to (u.login ?: "")
                }
            }
        }

        fun loadAvatars(context: Context, list: List<Movie>) {
            if (list.isEmpty()) return
            var hasUncached = false
            for (movie in list) {
                val uid = movie.userId
                if (uid <= 0) continue
                val cached = CACHE[uid]
                if (cached != null) {
                    movie.avatarUrl = cached.first
                    if (cached.second.isNotEmpty()) {
                        movie.login = cached.second
                    }
                } else {
                    hasUncached = true
                }
            }
            if (!hasUncached) return

            // Never block the main thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                val appCtx = context.applicationContext
                val copy = ArrayList(list)
                Thread({ loadAvatars(appCtx, copy) }, "Avatare").start()
                return
            }

            try {
                val seen = LinkedHashSet<Int>()
                val query = StringBuilder("?id=")
                var first = true
                for (m in list) {
                    val uid = m.userId
                    if (uid <= 0 || CACHE.containsKey(uid) || !seen.add(uid)) continue
                    if (!first) query.append("&id=")
                    query.append(uid)
                    first = false
                }
                if (first) return

                val usersByQuery = HelixApi.getUsersByQuery(context, query.toString()) ?: return
                val channels = HelixParser.parseUsers(usersByQuery)
                if (channels.isEmpty()) return
                cacheUsers(channels)
                for (movie in list) {
                    val cached = CACHE[movie.userId] ?: continue
                    movie.avatarUrl = cached.first
                    if (cached.second.isNotEmpty()) {
                        movie.login = cached.second
                    }
                }
            } catch (e: Exception) {
                CrashReporter.recordException(e)
            }
        }
    }
}
