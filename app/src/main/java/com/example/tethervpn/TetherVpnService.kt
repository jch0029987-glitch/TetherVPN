package com.example.tethervpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class TetherVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var process: Process? = null
    private val TAG = "TetherVPN"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getIntExtra("MODE", 1) ?: 1

        Log.d(TAG, "[VPN] Starting service mode=$mode")

        startVpn(mode)

        return START_STICKY
    }

    private fun startVpn(mode: Int) {
        stopVpn() // ensure clean restart

        try {
            val builder = Builder()
            builder.setSession("TetherVPN")

            builder.addAddress("10.0.0.2", 24)
            builder.addRoute("0.0.0.0", 0)

            // Optional DNS
            builder.addDnsServer("1.1.1.1")
            builder.addDnsServer("8.8.8.8")

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                Log.e(TAG, "[VPN] Failed to establish interface")
                return
            }

            Log.d(TAG, "[VPN] Interface established")

            val binary = copyBinary()

            val fd = vpnInterface!!.fileDescriptor

            process = ProcessBuilder(
                binary.absolutePath,
                fd.toString(),
                mode.toString()
            )
                .redirectErrorStream(true)
                .start()

            Log.d(TAG, "[VPN] Native process started")

            // Log native output
            Thread {
                process?.inputStream?.bufferedReader()?.forEachLine {
                    Log.d(TAG, "[C] $it")
                }
            }.start()

        } catch (e: Exception) {
            Log.e(TAG, "[VPN] Error: ${e.message}")
        }
    }

    private fun stopVpn() {
        try {
            Log.d(TAG, "[VPN] Stopping...")

            process?.destroy()
            process = null

            vpnInterface?.close()
            vpnInterface = null

        } catch (e: Exception) {
            Log.e(TAG, "[VPN] Stop error: ${e.message}")
        }
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun copyBinary(): File {
        val file = File(filesDir, "tor_gateway")

        if (!file.exists()) {
            Log.d(TAG, "[Binary] Copying...")

            assets.open("tor_gateway").use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.setExecutable(true)
        }

        Log.d(TAG, "[Binary] Ready at ${file.absolutePath}")
        return file
    }
}
