package com.mooncell07.cecc.core

class Bus(
    private val area: UByteArray = UByteArray(0x10000) { 0u },
) {
    fun readByte(address: UShort): UByte = area[address.toInt()]

    fun writeByte(
        address: UShort,
        data: UByte,
    ) {
        area[address.toInt()] = data
    }

    fun readWord(
        address: UShort,
        wrapping: Boolean = false,
    ): UShort {
        val lo = readByte(address)
        var hiAddr = (address + 1u)
        if (wrapping) {
            hiAddr %= 0x100u
        }
        val hi = readByte(hiAddr.toUShort())
        return concat(hi, lo)
    }
}
