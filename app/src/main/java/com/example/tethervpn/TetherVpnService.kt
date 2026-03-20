package com.example.tethervpn

import android.content.Intent
import android.net.VpnService
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
        val mode = intent?.getIntExtra("MODE", 1) ?: 1 // 1=Tor, 2=Direct, 3=Proxy

        setupVpn()
        copyBinary()
        startCustomNetworkStack(mode)

        return START_STICKY
    }

    private fun setupVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24)
        builder.addRoute("0.0.0.0", 0)
        vpnInterface = builder.establish()
        Log.d(TAG, "[VPN] Interface established")
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
                Log.d(TAG, "[Binary] Copied and executable: ${binaryFile.absolutePath}")
            } catch (e: IOException) {
                Log.e(TAG, "[Binary] Failed to copy: ${e.message}")
            }
        } else {
            Log.d(TAG, "[Binary] Already exists: ${binaryFile.absolutePath}")
        }
    }

    private fun startCustomNetworkStack(mode: Int) {
        vpnInterface?.fileDescriptor?.let { fd ->
            try {
                vpnProcess = ProcessBuilder(binaryFile.absolutePath, fd.toString(), mode.toString())
                    .redirectErrorStream(true)
                    .start()

                // Log output from the C stack
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
        vpnProcess?.destroy()
        vpnProcess = null
        Log.d(TAG, "[NetworkStack] Stopped")
    }

    override fun onDestroy() {
        stopCustomNetworkStack()
        vpnInterface?.close()
        super.onDestroy()
    }
}
