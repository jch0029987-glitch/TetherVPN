package com.example.tethervpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TetherVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private lateinit var binaryFile: File

    private val TAG = "TetherVpnService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setupVpn()
        copyAndCheckBinary()
        startTun2Socks()
        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
        Log.d(TAG, "VPN interface established")
    }

    private fun copyAndCheckBinary() {
        binaryFile = File(filesDir, "tor_gateway")
        if (!binaryFile.exists()) {
            try {
                assets.open("tor_gateway").use { input ->
                    FileOutputStream(binaryFile).use { output ->
                        input.copyTo(output)
                    }
                }
                binaryFile.setExecutable(true)
                Log.d(TAG, "Binary copied and made executable: ${binaryFile.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy binary: ${e.message}")
            }
        } else {
            Log.d(TAG, "Binary already exists: ${binaryFile.absolutePath}")
        }

        // Try loading library if applicable
        try {
            System.loadLibrary("tun2socks")
            Log.d(TAG, "tun2socks library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load tun2socks library: ${e.message}")
        }
    }

    private fun startTun2Socks() {
        vpnInterface?.fileDescriptor?.let {
            try {
                Tun2SocksJNI.start(it)
                Log.d(TAG, "Tun2Socks started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Tun2Socks: ${e.message}")
            }
        } ?: Log.e(TAG, "VPN interface file descriptor is null")
    }

    override fun onDestroy() {
        try {
            Tun2SocksJNI.stop()
            vpnInterface?.close()
            Log.d(TAG, "VPN stopped and interface closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN: ${e.message}")
        }
        super.onDestroy()
    }
}
