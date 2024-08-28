package com.mooncell07.cecc.core

open class Register {
    var pc: UShort = 0x0000u

    // First value is unused.
    private val registers: Array<UByte> = arrayOf(0x00u, 0x00u, 0x00u, 0x00u, 0xFDu, 0x24u)

    operator fun get(registerType: RegisterType): UByte {
        assert(registerType != RegisterType.NONE) { "$registerType type register is not supported for REGGET." }
        return registers[registerType.ordinal]
    }

    operator fun set(registerType: RegisterType, data: UByte) {
        assert(registerType != RegisterType.NONE) { "$registerType type register is not supported for REGSET." }
        registers[registerType.ordinal] = data
    }

    operator fun get(type: FlagType): Boolean {
        assert(type != FlagType.NONE) { "$type type flag is not supported for FLAGGET." }
        return testBit(registers[RegisterType.SR.ordinal].toInt(), getFlagOrdinal(type))
    }

    operator fun set(type: FlagType, value: Boolean) {
        assert(type != FlagType.NONE) { "$type type flag is not supported for FLAGSET." }
        val idx = RegisterType.SR.ordinal
        registers[idx] = handleBit(registers[idx].toInt(), getFlagOrdinal(type), value).toUByte()
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
