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
    private var vpnProcess: Process? = null
    private val TAG = "TetherVpnService"
    private lateinit var binaryFile: File

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getIntExtra("MODE", 1) ?: 1
        Log.d(TAG, "[VPN] Starting VPN service in mode $mode")

        setupVpn()
        copyBinary()
        startCustomNetworkStack(mode)

        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)

        // Kill switch: block non-VPN traffic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.allowBypass(false)
        }

        vpnInterface = builder.establish()
        if (vpnInterface != null) Log.d(TAG, "[VPN] Interface established")
        else Log.e(TAG, "[VPN] Failed to establish VPN interface")
    }

    private fun copyBinary() {
        binaryFile = File(filesDir, "tor_gateway")
        if (!binaryFile.exists()) {
            try {
                assets.open("tor_gateway").use { input ->
                    FileOutputStream(binaryFile).use { output ->
                        input.copyTo(output)
                    }
                }
                binaryFile.setExecutable(true)
                Log.d(TAG, "[Binary] Copied and executable")
            } catch (e: IOException) {
                Log.e(TAG, "[Binary] Failed to copy: ${e.message}")
            }
        } else Log.d(TAG, "[Binary] Already exists")
    }

    private fun startCustomNetworkStack(mode: Int) {
        vpnInterface?.fileDescriptor?.let { fd ->
            try {
                vpnProcess = ProcessBuilder(binaryFile.absolutePath, fd.toString(), mode.toString())
                    .redirectErrorStream(true)
                    .start()

                // Log output
                Thread {
                    vpnProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "[NetworkStack] $line")
                    }
                }.start()

                Log.d(TAG, "[NetworkStack] Started in mode $mode")
            } catch (e: Exception) {
                Log.e(TAG, "[NetworkStack] Failed to start: ${e.message}")
            }
        } ?: Log.e(TAG, "[NetworkStack] VPN interface fd is null")
    }

    private fun stopCustomNetworkStack() {
        vpnProcess?.let {
            if (it.isAlive) {
                Log.d(TAG, "[NetworkStack] Destroying C process…")
                it.destroy()
                it.waitFor()
            }
        }
        vpnProcess = null

        vpnInterface?.let {
            try {
                Log.d(TAG, "[VPN] Closing TUN interface…")
                it.close()
            } catch (e: Exception) {
                Log.e(TAG, "[VPN] Failed to close TUN: ${e.message}")
            }
        }
        vpnInterface = null
        Log.d(TAG, "[VPN] VPN service stopped")
    }

    override fun onDestroy() {
        stopCustomNetworkStack()
        super.onDestroy()
    }
}
