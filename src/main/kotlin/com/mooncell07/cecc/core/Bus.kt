@file:OptIn(ExperimentalUnsignedTypes::class)

package com.mooncell07.cecc.core

class Bus(
    private val cart: Cartridge,
    private val zeroPage: UByteArray = UByteArray(0xFF) { 0u },
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    fun readByte(address: UShort): UByte =
        if (address < 0x00FFu) {
            zeroPage[address.toInt()]
        } else if (0xC000u <= address && address <= 0xFFFFu) {
            cart.readRom((address - 0xC000u + 0x10u).toUShort())
        } else {
            throw IndexOutOfBoundsException("The address: $address is not mapped to any READABLE devices.")
        }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun writeByte(
        address: UShort,
        data: UByte,
    ) {
        if (address <= 0xFFu) {
            zeroPage[address.toInt()] = data
        } else if (0xC000u <= address && address <= 0xFFFFu) {
            cart.writeRom((address - 0xC000u).toUShort(), data)
        } else {
            throw IndexOutOfBoundsException("The address: $address is not mapped to any WRITEABLE devices.")
        }
    }

    fun readWord(address: UShort): UShort = concat(readByte((address + 1u).toUShort()), readByte(address))
}
