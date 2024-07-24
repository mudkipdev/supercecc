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

    fun readWord(address: UShort): UShort = concat(readByte((address + 1u).toUShort()), readByte(address))
}
