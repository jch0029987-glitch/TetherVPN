package com.example.tethervpn

import java.io.FileDescriptor

object Tun2SocksJNI {
    init {
        System.loadLibrary("tun2socks") // Loads the .so
    }

    // fd: the file descriptor from VPNService
    external fun start(fd: FileDescriptor)

    external fun stop()
}
