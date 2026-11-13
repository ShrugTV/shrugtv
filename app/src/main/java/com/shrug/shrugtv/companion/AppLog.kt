package com.shrug.shrugtv.companion

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ShrugTV's own log for the browser (tab "Log", like the Tools tab of Glasshouse). Reads the app's
 * logcat: an app without READ_LOGS only ever sees the lines of its own uid, so this is exactly
 * ShrugTV's log, including what was written before the browser opened (e.g. a chat that stayed
 * empty, or the crash of the previous run). Started on the first request, stopped after
 * [IDLE_STOP_MS] without one; the last [MAX_LINES] lines stay in memory.
 *
 * Tokens and secrets are always removed ([redact]); the browser can hide more (IP addresses …) when
 * copying or exporting.
 */
object AppLog {
    private const val TAG = "AppLog"
    const val MAX_LINES = 5_000
    private const val IDLE_STOP_MS = 5 * 60_000L

    data class Line(val seq: Long, val timeMs: Long, val level: Char, val tag: String, val pid: Int, val tid: Int, val msg: String)

    private val lines = ArrayDeque<Line>()
    private var nextSeq = 1L
    @Volatile private var process: Process? = null
    @Volatile private var lastUse = 0L

    /** `-v epoch`: "  1730412345.123  1234  5678 D Tag     : message". */
    private val FORMAT = Regex("^\\s*(\\d+)\\.(\\d{3})\\d*\\s+(\\d+)\\s+(\\d+)\\s+([VDIWEFA])\\s+(.*?)\\s*: (.*)$")

    @JvmStatic
    fun parse(raw: String, seq: Long): Line? {
        val m = FORMAT.matchEntire(raw) ?: return null
        val (sec, ms, pid, tid, level, tag, msg) = m.destructured
        return Line(seq, sec.toLong() * 1000 + ms.toLong(), level[0], tag.trim(), pid.toInt(), tid.toInt(), redact(msg))
    }

    private val SECRETS = listOf(
        Regex("(?i)(bearer|oauth)[ :]+[A-Za-z0-9._~+/=-]{8,}") to "$1 ***",
        Regex("(?i)\\b(access_token|refresh_token|client_secret|password|passwd|token)([\"']?\\s*[=:]\\s*[\"']?)[^\\s\"'&,;}]+") to "$1$2***",
        Regex("(?i)(shrugtv_sid=)[^;\\s]+") to "$1***",
        Regex("(?i)(/p/)[A-Za-z0-9_-]{16,}") to "$1***"
    )

    /** Removes tokens, passwords and pairing links; always, also in the live view. */
    @JvmStatic
    fun redact(msg: String): String = SECRETS.fold(msg) { s, (re, rep) -> re.replace(s, rep) }

    /** Lines after [after] (sequence number), at most [limit]; starts the reader if needed. */
    fun since(after: Long, limit: Int): Pair<List<Line>, Long> {
        lastUse = System.currentTimeMillis()
        ensureRunning()
        synchronized(lines) {
            val out = lines.filter { it.seq > after }.takeLast(limit.coerceIn(1, MAX_LINES))
            return out to (lines.firstOrNull()?.seq ?: nextSeq)
        }
    }

    val isRunning: Boolean get() = process != null

    @Synchronized
    private fun ensureRunning() {
        if (process != null) return
        val p = try {
            // Without READ_LOGS logcat only returns this app's own lines; it dumps the buffer, then follows
            ProcessBuilder("logcat", "-v", "epoch").redirectErrorStream(true).start()
        } catch (e: Exception) {
            Log.w(TAG, "logcat not available: ${e.message}")
            return
        }
        process = p
        Thread({ read(p) }, "AppLog reader").apply { isDaemon = true }.start()
        Thread({
            while (process === p) {
                Thread.sleep(30_000L)
                if (System.currentTimeMillis() - lastUse > IDLE_STOP_MS) stop()
            }
        }, "AppLog idle").apply { isDaemon = true }.start()
    }

    private fun read(p: Process) {
        try {
            BufferedReader(InputStreamReader(p.inputStream, Charsets.UTF_8)).use { r ->
                while (true) {
                    val raw = r.readLine() ?: break
                    add(raw)
                }
            }
        } catch (e: Exception) {
            // stopped
        } finally {
            if (process === p) process = null
        }
    }

    private fun add(raw: String) {
        synchronized(lines) {
            val line = parse(raw, nextSeq) ?: return
            // Our own reader would otherwise log about itself in a loop
            if (line.tag == TAG) return
            nextSeq++
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
    }

    @Synchronized
    fun stop() {
        process?.destroy()
        process = null
    }

    /** For tests. */
    internal fun addForTest(raw: String) = add(raw)

    internal fun clearForTest() = synchronized(lines) { lines.clear(); nextSeq = 1L }
}
