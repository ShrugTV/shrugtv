package com.shrug.shrugtv.activities

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import com.shrug.shrugtv.BuildConfig
import com.shrug.shrugtv.BuildConstants
import com.shrug.shrugtv.R
import com.shrug.shrugtv.session.AppState
import com.shrug.shrugtv.telemetry.CrashReporter
import com.shrug.shrugtv.util.AppLanguage
import java.net.Inet4Address
import java.net.NetworkInterface

class DebugActivity : Activity() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    private lateinit var appInfoList: ListView
    private lateinit var networkInfoList: ListView
    private lateinit var deviceInfoList: ListView
    private lateinit var streamInfoList: ListView

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setTheme(R.style.AppTheme_DarkGrey)
        theme.applyStyle(R.style.ThemeAccent_DarkGrey_Purple, true)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContentView(R.layout.activity_debug)

        appInfoList = findViewById(R.id.LV_AppInfo)
        networkInfoList = findViewById(R.id.LV_NetworkInfo)
        deviceInfoList = findViewById(R.id.LV_DeviceInfo)
        streamInfoList = findViewById(R.id.LV_StreamInfo)

        fillAllLists()
    }

    private fun fillAllLists() {
        fillAppInfo()
        fillDeviceInfo()
        fillNetworkInfo()
        fillStreamInfo()
    }

    private fun fillAppInfo() {
        val list = ArrayList<String>()
        list.add("Version name: ${BuildConfig.VERSION_NAME}")
        list.add("Version code: ${BuildConfig.VERSION_CODE}")
        list.add("User ID: ${AppState.hashedUserId}")
        list.add("Build type: ${BuildConfig.BUILD_TYPE.uppercase()}")
        list.add("Build time: ${BuildConstants.BUILD_TIME}")
        list.add("Application ID: ${BuildConfig.APPLICATION_ID}")
        appInfoList.adapter = ArrayAdapter(this, R.layout.list_view_item, list)
    }

    private fun fillDeviceInfo() {
        val list = ArrayList<String>()
        list.add("Brand: ${Build.BRAND}")
        list.add("Model: ${Build.MODEL}")
        list.add("Device: ${Build.DEVICE}")
        list.add("SDK: ${Build.VERSION.SDK_INT}")
        list.add("Android ${Build.VERSION.RELEASE}")
        if (Build.VERSION.SDK_INT >= 23) {
            list.add("Security patch: ${Build.VERSION.SECURITY_PATCH}")
        }
        list.add(Build.DISPLAY)
        list.add(Build.HOST)
        list.add(Build.ID)
        deviceInfoList.adapter = ArrayAdapter(this, R.layout.list_view_item, list)
    }

    private fun fillNetworkInfo() {
        val list = ArrayList<String>()
        list.add("Connection Type: ${getConnectionTypeName()}")
        list.addAll(getIpv4Addresses())
        networkInfoList.adapter = ArrayAdapter(this, R.layout.list_view_item, list)
    }

    private fun getIpv4Addresses(): List<String> {
        val list = ArrayList<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        list.add("${networkInterface.name} - ${address.hostAddress}")
                    }
                }
            }
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            e.printStackTrace()
        }
        return list
    }

    private fun getConnectionTypeName(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            cm?.activeNetworkInfo?.typeName ?: "Unknown"
        } catch (e: Exception) {
            CrashReporter.recordException(e)
            ""
        }
    }

    private fun fillStreamInfo() {
        val list = ArrayList<String>()
        list.add("Server ads: ${AppState.debugServerAds}")
        list.add("Show ads: ${AppState.debugShowAds}")
        list.add("Platform: ${AppState.debugPlatform}")
        list.add("Player type: ${AppState.debugPlayerType}")
        list.add("Param: ${AppState.debugParam}")
        streamInfoList.adapter = ArrayAdapter(this, R.layout.list_view_item, list)
    }
}
