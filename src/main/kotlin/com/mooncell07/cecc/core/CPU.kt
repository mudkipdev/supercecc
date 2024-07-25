package com.mooncell07.cecc.core

class CPU(
    private val bus: Bus,
) {
    val reg: Register = Register()
    var instr: INSTR = INSTAB[0xEA]

    private val executors: Map<IT, () -> Unit> =
        mapOf(
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
    private val decoders: Map<AM, () -> UByte> =
        mapOf(
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

    private val stack: UByteArray = UByteArray(0xFF) { 0u }

    private fun fetch(): UByte = bus.readByte(reg.PC++)

    private fun fetchWord(): UShort {
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

    private fun readSrc(mode: AddressingMode): UByte {
        if (mode == AM.INDIRECT || mode == AM.RELATIVE) {
            throw IllegalArgumentException("readSrc() does not support INDIRECT and RELATIVE modes.")
        }
        return decoders[mode]!!.invoke()
    }

    private fun getInd(): UShort = bus.readWord(fetchWord())

    private fun getRel(): UShort = fetch().toByte().toUShort()

    private fun writeDst(
        r: RT,
        value: UByte,
    ) {
        reg[r] = value
    }

    private fun writeDst(
        f: FT,
        value: Boolean,
    ): Unit = if (value) reg.setFlag(f) else reg.clearFlag(f)

    private fun push(data: UByte) {
        stack[reg[RT.SP].toInt()] = data
        reg[RT.SP]--
    }

    private fun pop(): UByte {
        val res = stack[reg[RT.SP].toInt()]
        reg[RT.SP]++
        return res
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
                else -> throw IllegalArgumentException("Unsupported Addressing Mode: ${instr.addrMode}")
            }
    }

    private fun opJSR() {
        push(LSB(reg.PC))
        push(MSB(reg.PC))
        opJMP()
    }

    private fun opNOP() {}

    private fun opSET() {
        reg.setFlag(instr.flagType)
    }

    private fun opBRANCH() {
        val offset = getRel()
        val flag =
            when (instr.insType) {
                IT.BRSET -> reg[instr.flagType] == 1
                IT.BRCLR -> reg[instr.flagType] == 0
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }

        if (flag) {
            reg.PC = (reg.PC + offset).toUShort()
        }
    }

    private fun opCLEAR() {
        reg.clearFlag(instr.flagType)
    }

    fun tick() {
        instr = INSTAB[fetch().toInt()]
        assert(instr.insType != IT.NONE) { "${instr.insType} is an illegal instruction type." }
        executors[instr.insType]?.invoke()
    }
}
