@file:OptIn(ExperimentalUnsignedTypes::class)

package cecc.core

class Bus(private val cart: Cartridge) {
    fun readByte(address: UShort): UByte {
        if (0xC010u <= address && address <= 0xFFFFu) {
            return cart.readRom((address - 0xC010u).toUShort())
        }
        return 0xFFu
    }

    fun writeByte(address: UShort, data: UByte): Unit {
        if (0xC010u <= address && address <= 0xFFFFu) {
            cart.writeRom((address - 0xC010u).toUShort(), data)
        }
    }

    fun readWord(address: UShort): UShort {
        return concat(readByte((address + 1u).toUShort()), readByte(address))
    }

}