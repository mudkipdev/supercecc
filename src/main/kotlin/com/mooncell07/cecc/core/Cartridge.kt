package com.mooncell07.cecc.core

import java.io.File

@ExperimentalUnsignedTypes
class Cartridge(filepath: String) {
    private val data: UByteArray = File(filepath).readBytes().toUByteArray()

    fun readRom(address: UShort): UByte {
        return data[address.toInt()]
    }

    fun writeRom(address: UShort, data: UByte): Unit {
        println("WRITE ROM IS NOT SUPPORTED.")
    }
}
