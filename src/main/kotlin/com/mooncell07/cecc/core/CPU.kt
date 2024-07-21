package com.mooncell07.cecc.core

open class Stream(
    val reg: Register,
    val bus: Bus,
) {
    private val addrHandlers: MutableMap<AM, () -> UByte>

    init {
        addrHandlers =
            mutableMapOf(
                AM.IMMEDIATE to { getImm() },
                AM.ACCUMULATOR to { getAcc() },
                AM.ABSOLUTE to { getAbs() },
                AM.ABSOLUTE_X to { getAbsX() },
                AM.ABSOLUTE_Y to { getAbsY() },
                AM.X_INDIRECT to { getXInd() },
                AM.INDIRECT_Y to { getIndY() },
                AM.ZEROPAGE to { getZP() },
                AM.ZEROPAGE_X to { getZPX() },
                AM.ZEROPAGE_Y to { getZPY() },
            )
    }

    fun fetch(): UByte = bus.readByte(reg.PC++)

    fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    private fun getImm(): UByte = fetch()

    private fun getAcc(): UByte = reg[RT.A]

    private fun getAbs(): UByte = bus.readByte(fetchWord())

    private fun getAbsX(): UByte = bus.readByte((fetchWord() + reg[RT.X]).toUShort())

    private fun getAbsY(): UByte = bus.readByte((fetchWord() + reg[RT.Y]).toUShort())

    private fun getXInd(): UByte {
        val base = (fetch() + reg[RT.X]).toUByte()
        val ptrAddr = concat((base + 1u).toUByte(), base)
        val ptr = bus.readWord(ptrAddr)
        return bus.readByte(ptr)
    }

    private fun getIndY(): UByte {
        val base = fetch()
        val ptrAddr = concat((base + 1u).toUByte(), base) + reg[RT.Y]
        val ptr = bus.readWord(ptrAddr.toUShort())
        return bus.readByte(ptr)
    }

    private fun getZP(): UByte = bus.readByte(fetch().toUShort())

    private fun getZPX(): UByte {
        val fullAddr = bus.readWord((fetch() + reg[RT.X]).toUShort())
        return bus.readByte(fullAddr)
    }

    private fun getZPY(): UByte {
        val fullAddr = bus.readWord((fetch() + reg[RT.Y]).toUShort())
        return bus.readByte(fullAddr)
    }

    fun readSrc(mode: AddressingMode): UByte {
        if (mode == AM.INDIRECT || mode == AM.RELATIVE) {
            throw IllegalArgumentException("readSrc() does not support INDIRECT and RELATIVE modes.")
        }
        return addrHandlers[mode]!!.invoke()
    }

    fun getInd(): UShort = bus.readWord(fetchWord())

    fun getRel(): Byte = fetch().toByte()

    fun writeDst(
        r: RT,
        value: UByte,
    ) {
        reg[r] = value
    }

    fun writeDst(
        f: FT,
        value: Boolean,
    ): Unit = if (value) reg.setFlag(f) else reg.clearFlag(f)
}

class CPU(
    reg: Register,
    bus: Bus,
) : Stream(reg, bus) {
    private var instr = INSTAB[0xEA]
    private val handlers: MutableMap<IT, () -> Unit>

    init {
        handlers =
            mutableMapOf(
                IT.LOAD to { opLOAD() },
                IT.STORE to { opSTORE() },
                IT.JMP to { opJMP() },
            )
    }

    private fun opLOAD() {
        writeDst(instr.regType, readSrc(instr.addrMode))
    }

    private fun opSTORE() {
        bus.writeByte(readSrc(instr.addrMode).toUShort(), reg[instr.regType])
    }

    private fun opJMP() {
        reg.PC =
            when (instr.addrMode) {
                AM.ABSOLUTE -> fetchWord()
                AM.INDIRECT -> getInd()
                else -> 0xFFu
            }
    }

    fun tick() {
        val op = fetch().toInt()
        instr = INSTAB[op]
        handlers[instr.insType]?.invoke()
    }
}
