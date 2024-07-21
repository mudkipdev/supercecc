@file:OptIn(ExperimentalUnsignedTypes::class)

package com.mooncell07.cecc.core

class Bus(
    private val cart: Cartridge,
    private val zeroPage: UByteArray = UByteArray(0xFF) { 0u },
) {
    fun readByte(address: UShort): UByte {
        if (address < 0xFFu) {
            return zeroPage[address.toInt()]
        }
        if (0xC000u <= address && address <= 0xFFFFu) {
            return cart.readRom((address - 0xC000u + 0x10u).toUShort())
        }

        return 0xFFu
    }

    fun writeByte(
        address: UShort,
        data: UByte,
    ) {
        if (address < 0xFFu) {
            zeroPage[address.toInt()] = data
        }
        if (0xC000u <= address && address <= 0xFFFFu) {
            cart.writeRom((address - 0xC000u).toUShort(), data)
        }
    }

    fun readWord(address: UShort): UShort = concat(readByte((address + 1u).toUShort()), readByte(address))
}
