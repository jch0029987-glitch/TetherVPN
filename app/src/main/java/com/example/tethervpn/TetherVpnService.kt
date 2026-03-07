package com.example.tethervpn

import android.net.VpnService
import android.os.ParcelFileDescriptor

class TetherVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    init { System.loadLibrary("tun2socks") }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        setupVpn()
        startTun2Socks()
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
    }

    private fun startTun2Socks() {
        vpnInterface?.fileDescriptor?.let {
            Tun2SocksJNI.start(it)
        }
    }

    override fun onDestroy() {
        Tun2SocksJNI.stop()
        vpnInterface?.close()
        super.onDestroy()
    }
}
