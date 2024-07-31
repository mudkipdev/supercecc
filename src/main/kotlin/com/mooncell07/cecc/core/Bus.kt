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

    fun readWord(address: UShort): UShort {
        val lo = readByte(address)
        val hi = readByte(((address + 1u) % 0x100u).toUShort())
        return concat(hi, lo)
    }
}
