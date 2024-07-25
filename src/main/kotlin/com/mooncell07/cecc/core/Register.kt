package com.mooncell07.cecc.core

class Register {
    var PC: UShort = 0xC000u

    // First value is unused.
    private val regs: Array<UByte> = arrayOf(0x00u, 0x00u, 0x00u, 0x00u, 0xFDu, 0x24u)

    operator fun get(regType: RT): UByte {
        assert(regType != RT.NONE) { "$regType type register is not supported for REGGET." }
        return regs[regType.ordinal]
    }

    operator fun set(
        regType: RT,
        data: UByte,
    ) {
        assert(regType != RT.NONE) { "$regType type register is not supported for REGSET." }
        regs[regType.ordinal] = data
    }

    operator fun get(flagType: FT): Int {
        assert(flagType != FT.NONE) { "$flagType type flag is not supported for FLAGGET." }
        return testBit(regs[RT.SR.ordinal].toInt(), flagType.ordinal - 1)
    }

    fun setFlag(flagType: FT) {
        assert(flagType != FT.NONE) { "$flagType type flag is not supported for FLAGSET." }
        val idx = RT.SR.ordinal
        regs[idx] = setBit(regs[idx].toInt(), flagType.ordinal - 1).toUByte()
    }

    fun clearFlag(flagType: FT) {
        assert(flagType != FT.NONE) { "$flagType type flag is not supported for FLAGCLEAR." }
        val idx = RT.SR.ordinal
        regs[idx] = clearBit(regs[idx].toInt(), flagType.ordinal - 1).toUByte()
    }
}
