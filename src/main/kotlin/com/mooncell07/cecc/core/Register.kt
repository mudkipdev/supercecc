package com.mooncell07.cecc.core

open class Register {
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

    operator fun get(flagType: FT): Boolean {
        assert(flagType != FT.NONE) { "$flagType type flag is not supported for FLAGGET." }
        return testBit(regs[RT.SR.ordinal].toInt(), getFlagOrdinal(flagType))
    }

    operator fun set(
        flagType: FT,
        flagv: Boolean,
    ) {
        assert(flagType != FT.NONE) { "$flagType type flag is not supported for FLAGSET." }
        val idx = RT.SR.ordinal
        regs[idx] = handleBit(regs[idx].toInt(), getFlagOrdinal(flagType), flagv).toUByte()
    }

    @JvmName("kotlin-setPC")
    fun setPC(v: UShort) {
        PC = v
    }

    fun getFlagOrdinal(f: FT) = f.ordinal - 1

    fun checkOverflow(
        opA: UByte,
        opB: UByte,
        res: UByte,
        sbc: Boolean = false,
    ) {
        val opAMSb = testBit(opA.toInt(), 7)
        val opBMSb = testBit(opB.toInt(), 7)
        val opRes = testBit(res.toInt(), 7)
        if (sbc) {
            this[FT.V] = (opAMSb != opBMSb) and (opRes == opBMSb)
            return
        }
        this[FT.V] = (opAMSb == opBMSb) and (opRes != opAMSb)
    }
}
