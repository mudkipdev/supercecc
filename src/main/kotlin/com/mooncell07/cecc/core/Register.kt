package com.mooncell07.cecc.core

open class Register {
    var PC: UShort = 0x0000u

    // First value is unused.
    private val regs: Array<UByte> = arrayOf(0x00u, 0x00u, 0x00u, 0x00u, 0xFDu, 0x24u)

    operator fun get(regType: RegisterType): UByte {
        assert(regType != RegisterType.NONE) { "$regType type register is not supported for REGGET." }
        return regs[regType.ordinal]
    }

    operator fun set(
        regType: RegisterType,
        data: UByte,
    ) {
        assert(regType != RegisterType.NONE) { "$regType type register is not supported for REGSET." }
        regs[regType.ordinal] = data
    }

    operator fun get(flagType: FlagType): Boolean {
        assert(flagType != FlagType.NONE) { "$flagType type flag is not supported for FLAGGET." }
        return testBit(regs[RegisterType.SR.ordinal].toInt(), getFlagOrdinal(flagType))
    }

    operator fun set(
        flagType: FlagType,
        flagv: Boolean,
    ) {
        assert(flagType != FlagType.NONE) { "$flagType type flag is not supported for FLAGSET." }
        val idx = RegisterType.SR.ordinal
        regs[idx] = handleBit(regs[idx].toInt(), getFlagOrdinal(flagType), flagv).toUByte()
    }

    @JvmName("kotlin-setPC")
    fun setPC(v: UShort) {
        PC = v
    }

    fun getFlagOrdinal(f: FlagType) = f.ordinal - 1

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
            this[FlagType.V] = (opAMSb != opBMSb) and (opRes == opBMSb)
            return
        }
        this[FlagType.V] = (opAMSb == opBMSb) and (opRes != opAMSb)
    }
}
