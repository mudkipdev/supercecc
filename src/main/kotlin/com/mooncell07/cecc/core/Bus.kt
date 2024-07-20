@file:OptIn(ExperimentalUnsignedTypes::class)

package com.mooncell07.cecc.core

class Bus(
    private val cart: Cartridge,
) {
    fun readByte(address: UShort): UByte {
        if (0xC000u <= address && address <= 0xFFFFu) {
            return cart.readRom((address - 0xC000u).toUShort())
        }

        return 0xFFu
    }

    fun writeByte(
        address: UShort,
        data: UByte,
    ) {
        if (0xC000u <= address && address <= 0xFFFFu) {
            cart.writeRom((address - 0xC000u).toUShort(), data)
        }
    }

    fun readWord(address: UShort): UShort = concat(readByte((address + 1u).toUShort()), readByte(address))
}
