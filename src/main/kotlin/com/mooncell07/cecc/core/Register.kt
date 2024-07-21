
package com.mooncell07.cecc.core

class Register {
    var PC: UShort = 0xC000u

    private val regs = Array<UByte>(5) { 0u }

    operator fun get(regType: RT): UByte = regs[regType.ordinal]

    operator fun set(
        regType: RT,
        data: UByte,
    ) {
        regs[regType.ordinal] = data
    }

    operator fun get(flagType: FT): Int = (regs[RT.SR.ordinal].toInt() ushr flagType.ordinal)

    fun setFlag(flagType: FT) {
        val idx = RT.SR.ordinal
        regs[idx] = setBit(regs[idx].toInt(), flagType.ordinal).toUByte()
    }

    fun clearFlag(flagType: FT) {
        val idx = RT.SR.ordinal
        regs[idx] = clearBit(regs[idx].toInt(), flagType.ordinal).toUByte()
    }
}
