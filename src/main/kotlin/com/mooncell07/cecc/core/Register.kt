
package com.mooncell07.cecc.core

class Register {
    @Suppress("ktlint:standard:property-naming", "PropertyName")
    var PC: UShort = 0xC000u

    // First value is unused.
    val regs: Array<UByte> = arrayOf(0x00u, 0x00u, 0x00u, 0x00u, 0xFDu, 0x24u)

    operator fun get(regType: RT): UByte {
        if (regType == RT.NONE) {
            throw IllegalAccessError("Invalid READ attempt for Register Type `NONE`.")
        }
        return regs[regType.ordinal]
    }

    operator fun set(
        regType: RT,
        data: UByte,
    ) {
        if (regType == RT.NONE) {
            throw IllegalAccessError("Invalid WRITE attempt for Register Type `NONE`.")
        }
        regs[regType.ordinal] = data
    }

    operator fun get(flagType: FT): Int {
        if (flagType == FT.NONE) {
            throw IllegalAccessError("Invalid FLAGGET attempt for Register Type `NONE`.")
        }
        return testBit(regs[RT.SR.ordinal].toInt(), flagType.ordinal - 1)
    }

    fun setFlag(flagType: FT) {
        if (flagType == FT.NONE) {
            throw IllegalAccessError("Invalid FLAGSET attempt for Register Type `NONE`.")
        }
        val idx = RT.SR.ordinal
        regs[idx] = setBit(regs[idx].toInt(), flagType.ordinal - 1).toUByte()
    }

    fun clearFlag(flagType: FT) {
        if (flagType == FT.NONE) {
            throw IllegalAccessError("Invalid FLAGCLEAR attempt for Register Type `NONE`.")
        }
        val idx = RT.SR.ordinal
        regs[idx] = clearBit(regs[idx].toInt(), flagType.ordinal - 1).toUByte()
    }
}
