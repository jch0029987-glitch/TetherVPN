package com.example.tethervpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlin.concurrent.thread

class TetherVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra("mode") ?: "DIRECT"
        Log.d("VPN", "Starting VPN mode=$mode")

        setupVpn()

        vpnInterface?.fileDescriptor?.let { fd ->
            // Pass the raw fd to JNI
            thread {
                try {
                    NativeLib.startVpn(fd.fd)
                    Log.d("VPN", "VPN started successfully")
                } catch (e: Exception) {
                    Log.e("VPN", "Failed to start VPN", e)
                }
            }
        }

        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.setSession("TetherVPN")
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
        Log.d("VPN", "VPN interface established")
    }

    override fun onDestroy() {
        vpnInterface?.close()
        try {
            NativeLib.stopVpn()
            Log.d("VPN", "VPN stopped")
        } catch (e: Exception) {
            Log.e("VPN", "Failed to stop VPN", e)
        }
        super.onDestroy()
    }
}
