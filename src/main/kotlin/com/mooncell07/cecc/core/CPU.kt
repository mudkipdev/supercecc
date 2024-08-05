package com.mooncell07.cecc.core

class CPU(
    private val bus: Bus,
) {
    val reg: Register = Register()
    var instr: INSTR = INSTAB[0xEA]
    private var lastFetchedAddr: UShort = 0x0000u

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
            IT.TAX to { opTRANSFER() },
            IT.TAY to { opTRANSFER() },
            IT.TSX to { opTRANSFER() },
            IT.TXA to { opTRANSFER() },
            IT.TYA to { opTRANSFER() },
            IT.TXS to { opTRANSFER() },
            IT.PUSH to { opPUSH() },
            IT.PULL to { opPULL() },
            IT.INCREMENT to { opINCREMENT() },
            IT.DECREMENT to { opDECREMENT() },
            IT.ADC to { opADC() },
            IT.SBC to { opSBC() },
        )
    private val decoders: Map<AM, () -> UShort> =
        mapOf(
            AM.ABSOLUTE to { getAbs() },
            AM.ABSOLUTE_X to { getAbsX() },
            AM.ABSOLUTE_Y to { getAbsY() },
            AM.X_INDIRECT to { getXInd() },
            AM.INDIRECT_Y to { getIndY() },
            AM.ZEROPAGE to { getZP() },
            AM.ZEROPAGE_X to { getZPX() },
            AM.ZEROPAGE_Y to { getZPY() },
            AM.RELATIVE to { getRel() },
            AM.INDIRECT to { getInd() },
        )

    private val stack: UByteArray = UByteArray(0xFF) { 0u }

    private fun fetch(): UByte = bus.readByte(reg.PC++)

    private fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    // Only works for instrs that use pre defined regs
    private fun getImpl(): UByte = reg[instr.regType]

    private fun getImm(): UByte = fetch()

    private fun getAcc(): UByte = reg[RT.A]

    private fun getAbs(): UShort = fetchWord()

    private fun getAbsX(): UShort = (fetchWord() + reg[RT.X]).toUShort()

    private fun getAbsY(): UShort = (fetchWord() + reg[RT.Y]).toUShort()

    private fun getZP(): UShort = fetch().toUShort()

    private fun getZPX(): UShort = ((fetch() + reg[RT.X]) % 0x100u).toUShort()

    private fun getZPY(): UShort = ((fetch() + reg[RT.Y]) % 0x100u).toUShort()

    private fun getInd(): UShort {
        val base = fetchWord()
        val lo = bus.readByte(base)
        val hi =
            if (LSB(base).toUInt() == 0xFFu) {
                bus.readByte((base and 0xFF00u))
            } else {
                bus.readByte((base + 1u).toUShort())
            }
        return concat(hi, lo)
    }

    private fun getRel(): UShort = fetch().toByte().toUShort()

    private fun getXInd(): UShort {
        val base = (fetch() + reg[RT.X]) % 0x100u
        return bus.readWord(base.toUShort(), wrapping = true)
    }

    private fun getIndY(): UShort {
        val base = fetch()
        return (bus.readWord(base.toUShort(), wrapping = true) + reg[RT.Y].toUShort()).toUShort()
    }

    private fun readSrc8(mode: AddressingMode): UByte {
        if (mode == AM.INDIRECT || mode == AM.RELATIVE) {
            throw IllegalArgumentException("readSrc() does not support INDIRECT and RELATIVE modes.")
        }
        return when (mode) {
            AM.IMMEDIATE -> getImm()
            AM.ACCUMULATOR -> getAcc()
            AM.IMPLIED -> getImpl()
            else -> {
                lastFetchedAddr = decoders[mode]!!.invoke()
                bus.readByte(lastFetchedAddr)
            }
        }
    }

    private fun readSrc16(mode: AddressingMode): UShort = decoders[mode]!!.invoke()

    private fun push(data: UByte) {
        bus.writeByte((reg[RT.SP] + 0x100u).toUShort(), data)
        reg[RT.SP]--
    }

    private fun pop(): UByte {
        reg[RT.SP]++
        val res = bus.readByte((reg[RT.SP] + 0x100u).toUShort())
        return res
    }

    private fun opLOAD() {
        val data = readSrc8(instr.addrMode)
        reg[instr.regType] = data
        reg[FT.N] = testBit(data.toInt(), 7)
        reg[FT.Z] = data.toInt() == 0
    }

    private fun opSTORE() {
        bus.writeByte(readSrc16(instr.addrMode), reg[instr.regType])
    }

    private fun opJMP() {
        reg.PC = readSrc16(instr.addrMode)
    }

    private fun opJSR() {
        val pc = (reg.PC + 1u).toUShort()
        push(MSB(pc))
        push(LSB(pc))
        reg.PC = readSrc16(instr.addrMode)
    }

    private fun opNOP() {}

    private fun opSET() {
        reg[instr.flagType] = true
    }

    private fun opBRANCH() {
        val offset = readSrc16(instr.addrMode)
        val flag =
            when (instr.insType) {
                IT.BRSET -> reg[instr.flagType]
                IT.BRCLR -> !reg[instr.flagType]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }

        if (flag) {
            reg.PC = (reg.PC + offset).toUShort()
        }
    }

    private fun opCLEAR() {
        reg[instr.flagType] = false
    }

    private fun opTRANSFER() {
        val data =
            when (instr.insType) {
                IT.TXA -> reg[RT.X]
                IT.TYA -> reg[RT.Y]
                IT.TXS -> reg[RT.X]
                IT.TAY -> reg[RT.A]
                IT.TAX -> reg[RT.A]
                IT.TSX -> reg[RT.SP]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        reg[instr.regType] = data

        if (instr.insType != IT.TXS) {
            reg[FT.N] = testBit(data.toInt(), 7)
            reg[FT.Z] = data.toInt() == 0
        }
    }

    private fun opPUSH() {
        var data = reg[instr.regType]
        if (instr.regType == RT.SR) {
            data = setBit(data.toInt(), FT.B.ordinal - 1).toUByte()
        }
        push(data)
    }

    private fun opPULL() {
        val data = pop()
        reg[instr.regType] = data

        if (instr.regType == RT.SR) {
            reg[FT.B] = false
            reg[FT.UNUSED2_IGN] = true
        } else {
            reg[FT.N] = testBit(data.toInt(), 7)
            reg[FT.Z] = data.toInt() == 0
        }
    }

    private fun opINCREMENT() {
        var data = readSrc8(instr.addrMode)
        data++

        when (instr.addrMode) {
            AM.IMPLIED -> reg[instr.regType] = data
            else -> bus.writeByte(lastFetchedAddr, data)
        }

        reg[FT.N] = testBit(data.toInt(), 7)
        reg[FT.Z] = data.toInt() == 0
    }

    private fun opDECREMENT() {
        var data = readSrc8(instr.addrMode)
        data--

        when (instr.addrMode) {
            AM.IMPLIED -> reg[instr.regType] = data
            else -> bus.writeByte(lastFetchedAddr, data)
        }

        reg[FT.N] = testBit(data.toInt(), 7)
        reg[FT.Z] = data.toInt() == 0
    }

    private fun opADC() {
        val acc = reg[RT.A]
        val mem = readSrc8(instr.addrMode)
        val c = (if (reg[FT.C]) 1u else 0u)
        val data = (mem + acc + c).toInt()
        reg[FT.C] = data > 0xFF
        reg[FT.Z] = (data % 0x100) == 0
        reg[FT.N] = testBit(data, 7)
        reg[RT.A] = data.toUByte()
        reg.checkOverflow(acc, mem, data.toUByte())
    }

    private fun opSBC() {
        val acc = reg[RT.A]
        val mem = readSrc8(instr.addrMode)
        val c = (if (reg[FT.C]) 0u else 1u)
        val data = (acc - mem - c).toInt()
        reg[FT.C] = data >= 0
        reg[FT.Z] = (data % 0x100) == 0
        reg[FT.N] = testBit(data, 7)
        reg[RT.A] = data.toUByte()
        reg.checkOverflow(acc, mem, data.toUByte(), sbc = true)
    }

    fun tick() {
        instr = INSTAB[fetch().toInt()]
        assert(instr.insType != IT.NONE) { "${instr.insType} is an illegal instruction type." }
        executors[instr.insType]?.invoke()
    }
}
