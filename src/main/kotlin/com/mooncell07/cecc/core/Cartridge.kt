package com.mooncell07.cecc.core

import java.io.File

class Cartridge(
    filepath: String,
) : AbstractDevice() {
    override val type = DT.CARTRIDGE
    override val size = 0x8000
    override val base = 0x8000
    private val data: UByteArray = File(filepath).readBytes().toUByteArray()

    override fun read(address: UShort): UByte = data[address.toInt()]

    override fun write(
        address: UShort,
        data: UByte,
    ) {
        println("WRITE ROM IS NOT SUPPORTED.")
    }
}
