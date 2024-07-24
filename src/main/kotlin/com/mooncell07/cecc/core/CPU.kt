package com.mooncell07.cecc.core

open class Stream(
    val reg: Register,
    val bus: Bus,
) {
    private val addrHandlers: MutableMap<AM, () -> UByte>
    private val stack: MutableList<UByte> = MutableList(0xFF) { 0u }

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

    fun getRel(): UShort = fetch().toByte().toUShort()

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

    fun push(data: UByte) {
        stack[reg[RT.SP].toInt()] = data
        reg[RT.SP]--
    }

    fun pop(): UByte {
        val res = stack[reg[RT.SP].toInt()]
        reg[RT.SP]++
        return res
    }
}

class CPU(
    reg: Register = Register(),
    bus: Bus,
) : Stream(reg, bus) {
    var instr = INSTAB[0xEA]
    private val handlers: MutableMap<IT, () -> Unit>

    init {
        handlers =
            mutableMapOf(
                IT.LOAD to { opLOAD() },
                IT.STORE to { opSTORE() },
                IT.JMP to { opJMP() },
                IT.JSR to { opJSR() },
                IT.NOP to { opNOP() },
                IT.SET to { opSET() },
                IT.BRCLR to { opBRANCH() },
                IT.BRSET to { opBRANCH() },
                IT.CLEAR to { opCLEAR() },
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

    private fun opJSR() {
        push(LSB(reg.PC))
        push(MSB(reg.PC))
        opJMP()
    }

    private fun opNOP() {
        return
    }

    private fun opSET() {
        reg.setFlag(instr.flagType)
    }

    private fun opBRANCH() {
        val offset = getRel()
        when (instr.insType) {
            IT.BRSET -> {
                if (reg[instr.flagType] == 1) {
                    reg.PC = (reg.PC + offset).toUShort()
                }
            }
            IT.BRCLR -> {
                if (reg[instr.flagType] == 0) {
                    reg.PC = (reg.PC + offset).toUShort()
                }
            }
            else -> println("Unsupported type.")
        }
    }

    private fun opCLEAR() {
        reg.clearFlag(instr.flagType)
    }

    fun tick() {
        val op = fetch().toInt()
        instr = INSTAB[op]
        handlers[instr.insType]?.invoke()
    }
}
