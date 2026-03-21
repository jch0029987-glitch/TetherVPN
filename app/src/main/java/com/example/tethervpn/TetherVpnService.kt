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
            thread {
                NativeLib.startVpn(fd.detachFd())
            }
        }

        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.setSession("TetherVPN")
        vpnInterface = builder.establish()
    }

    override fun onDestroy() {
        vpnInterface?.close()
        NativeLib.stopVpn()
        super.onDestroy()
    }
}
