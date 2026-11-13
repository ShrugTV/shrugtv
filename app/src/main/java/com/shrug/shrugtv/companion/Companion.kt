package com.shrug.shrugtv.companion

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.shrug.shrugtv.R
import java.net.Inet4Address

/**
 * The phone companion (docs/companion_plan.md): runs [CompanionServer] while ShrugTV is in the
 * foreground (and 2 minutes after) and "Phone companion" is on; builds the pairing address for the
 * QR code. Sessions live in memory; devices paired while "Remember connected devices" is on keep a
 * key and reconnect without a code (TrustedDevices).
 */
object Companion : DefaultLifecycleObserver {
    private const val TAG = "Companion"
    const val PORT = 8765
    private const val LINGER_MS = 120_000L

    private lateinit var appContext: Context
    private val main = Handler(Looper.getMainLooper())
    val pairing = Pairing()

    /** Remembered phones and computers; `CompanionDevices.xml` is excluded from backups. */
    val devices: TrustedDevices by lazy {
        val prefs = appContext.getSharedPreferences("CompanionDevices", Context.MODE_PRIVATE)
        TrustedDevices(object : TrustedDevices.Store {
            override fun load(): String? = prefs.getString("devices", null)
            override fun save(json: String) { prefs.edit().putString("devices", json).apply() }
        })
    }
    @Volatile private var server: CompanionServer? = null
    private val stopRunnable = Runnable { stop() }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pref_companion_enabled") {
            if (isEnabled()) start() else stop()
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        PreferenceManager.getDefaultSharedPreferences(appContext).registerOnSharedPreferenceChangeListener(prefListener)
    }

    fun isEnabled(): Boolean = PreferenceManager.getDefaultSharedPreferences(appContext).getBoolean("pref_companion_enabled", false)

    override fun onStart(owner: LifecycleOwner) {
        main.removeCallbacks(stopRunnable)
        if (isEnabled()) start()
    }

    override fun onStop(owner: LifecycleOwner) {
        main.removeCallbacks(stopRunnable)
        main.postDelayed(stopRunnable, LINGER_MS)
    }

    @Synchronized
    fun start() {
        if (server != null || !isEnabled()) return
        try {
            val s = CompanionServer(appContext, PORT, pairing, devices, { main.postDelayed({ restart() }, 500L) }) { phone -> onPaired(phone) }
            s.start(0, true) // no read timeout: WebSockets stay open
            server = s
            Log.i(TAG, "Listening on port $PORT")
        } catch (e: Exception) {
            Log.w(TAG, "Cannot start the companion server", e)
        }
    }

    @Synchronized
    fun stop() {
        main.removeCallbacks(stopRunnable)
        server?.shutdown()
        server = null
        // Sessions end with the server (memory only)
        pairing.disconnectAll()
    }

    /** "Forget all devices": ends every session and removes the remembered devices. */
    fun disconnectAll() {
        pairing.disconnectAll()
        devices.clear()
        restart()
    }

    /** Removes one remembered device (TV list); its open connection ends. */
    fun forget(id: String) {
        if (devices.remove(id)) restart()
    }

    /** Open WebSockets close with a restart; devices still allowed reconnect by themselves. */
    private fun restart() {
        if (server != null) { stop(); start() }
    }

    /** IPv4 address of the active Wi-Fi/Ethernet network, or null. */
    fun lanAddress(): String? {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return null
        val lp: LinkProperties = cm.getLinkProperties(cm.activeNetwork) ?: return null
        return lp.linkAddresses.map { it.address }.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }

    /** A fresh offer: (address for the QR code, plain address, PIN), or null without network/server. */
    fun newPairing(): Triple<String, String, String>? {
        start()
        if (server == null) return null
        val ip = lanAddress() ?: return null
        val offer = pairing.newOffer()
        val base = "http://$ip:$PORT"
        return Triple("$base/p/${offer.token}", base, offer.pin)
    }

    private fun onPaired(phone: Pairing.Phone) {
        // Nobody controls the TV unnoticed
        val device = phone.userAgent.substringAfter("(").substringBefore(")").split(";").map { it.trim() }
            .lastOrNull { it.isNotEmpty() && !it.startsWith("Linux") && !it.startsWith("U") }?.take(40) ?: "?"
        main.post {
            Toast.makeText(appContext, appContext.getString(R.string.companion_phone_connected, device), Toast.LENGTH_LONG).show()
        }
    }
}
