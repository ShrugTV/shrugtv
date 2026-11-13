package com.shrug.shrugtv.ui.tv.home

import android.content.Context
import android.util.Log
import com.shrug.shrugtv.model.Movie
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Last home screen in the app cache: on start it shows immediately while the fresh
 * rows load; before, the home screen stayed empty for 1.2-1.6 s. Belongs to a user id so nothing from
 * another account shows after switching accounts. Movie is Serializable (goes to player and channel
 * page via intent anyway).
 */
object HomeCache {
    private const val TAG = "HomeSpeicher"
    // 2: Movie with java.time instead of Joda
    // 3: row id stored next to the localized title
    private const val VERSION = 3

    private fun file(ctx: Context) = File(ctx.cacheDir, "home.bin")

    fun load(ctx: Context, user: String): List<HomeRow> = try {
        ObjectInputStream(BufferedInputStream(FileInputStream(file(ctx)))).use { input ->
            if (input.readInt() != VERSION || input.readUTF() != user) return emptyList()
            val count = input.readInt()
            List(count) {
                val id = input.readUTF()
                val title = input.readUTF()
                @Suppress("UNCHECKED_CAST")
                HomeRow(id, title, input.readObject() as ArrayList<Movie>)
            }
        }
    } catch (e: java.io.FileNotFoundException) {
        emptyList()
    } catch (e: Exception) {
        Log.w(TAG, "Gespeicherte Startseite unlesbar", e)
        file(ctx).delete()
        emptyList()
    }

    fun save(ctx: Context, user: String, rows: List<HomeRow>) {
        val target = file(ctx)
        val updated = File(ctx.cacheDir, "home.bin.new")
        try {
            ObjectOutputStream(BufferedOutputStream(FileOutputStream(updated))).use { out ->
                out.writeInt(VERSION)
                out.writeUTF(user)
                out.writeInt(rows.size)
                for (z in rows) {
                    out.writeUTF(z.id)
                    out.writeUTF(z.title)
                    out.writeObject(ArrayList(z.entries))
                }
            }
            if (!updated.renameTo(target)) {
                target.delete()
                updated.renameTo(target)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Home screen not saved", e)
            updated.delete()
        }
    }

    fun delete(ctx: Context) {
        file(ctx).delete()
    }
}
