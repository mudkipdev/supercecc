@file:OptIn(ExperimentalUnsignedTypes::class)

package com.mooncell07.cecc.core

class Register {
    var PC: UShort = 0xFFFCu
    private val regs = Array<UByte>(5) { 0u }

    operator fun get(regType: RegType): UByte {
        return regs[regType.ordinal]
    }

    operator fun set(regType: RegType, data: UByte): Unit {
        regs[regType.ordinal] = data
    }

    operator fun get(flagType: FlagType): Int {
        return (regs[RegType.SR.ordinal].toInt() ushr flagType.ordinal)
    }

    fun setFlag(flagType: FlagType) {
        val idx = RegType.SR.ordinal
        regs[idx] = setBit(regs[idx].toInt(), flagType.ordinal).toUByte()
    }

    fun clearFlag(flagType: FlagType) {
        val idx = RegType.SR.ordinal
        regs[idx] = clearBit(regs[idx].toInt(), flagType.ordinal).toUByte()
    }
}
