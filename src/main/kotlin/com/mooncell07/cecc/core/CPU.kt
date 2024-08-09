package com.mooncell07.cecc.core

class CPU(
    val bus: Bus,
) {
    val reg: Register = Register()
    var instr: INSTR = INSTAB[0xEA]
    private var lastAddr: UShort = 0x0000u
    private val stack: UByteArray = UByteArray(0xFF) { 0u }

    // Fetchers
    // ------------------------------------------------------------------------------------

    private fun fetch(): UByte = bus.readByte(reg.PC++)

    private fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    // ------------------------------------------------------------------------------------

    // Handlers for different addressing modes
    // ------------------------------------------------------------------------------------

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

    private fun getRel(): UShort = fetch().toByte().toUShort()

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

    private fun getXInd(): UShort {
        val addr = fetch()
        bus.dummyRead(addr.toUShort())
        val base = (addr + reg[RT.X]) % 0x100u
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
                lastAddr = decoders[mode]!!.invoke()
                bus.readByte(lastAddr)
            }
        }
    }

    private fun readSrc16(mode: AddressingMode): UShort = decoders[mode]!!.invoke()

    // ------------------------------------------------------------------------------------

    // Utils
    // ------------------------------------------------------------------------------------

    private fun push(data: UByte) {
        bus.writeByte((reg[RT.SP] + 0x100u).toUShort(), data)
        reg[RT.SP]--
    }

    private fun pop(): UByte {
        reg[RT.SP]++
        val v = bus.readByte((reg[RT.SP] + 0x100u).toUShort())
        return v
    }

    private fun indcr(v: UByte) {
        when (instr.addrMode) {
            AM.IMPLIED -> reg[instr.regType] = v
            else -> bus.writeByte(lastAddr, v)
        }
        reg[FT.N] = testBit(v.toInt(), 7)
        reg[FT.Z] = v.toInt() == 0
    }

    // ------------------------------------------------------------------------------------

    // Instruction Handlers
    // ------------------------------------------------------------------------------------

    private fun opLOAD() {
        val m = readSrc8(instr.addrMode)
        reg[instr.regType] = m
        reg[FT.N] = testBit(m.toInt(), 7)
        reg[FT.Z] = m.toInt() == 0
    }

    private fun opSTORE() {
        bus.writeByte(readSrc16(instr.addrMode), reg[instr.regType])
    }

    private fun opTRANSFER() {
        val r =
            when (instr.insType) {
                IT.TXA -> reg[RT.X]
                IT.TYA -> reg[RT.Y]
                IT.TXS -> reg[RT.X]
                IT.TAY -> reg[RT.A]
                IT.TAX -> reg[RT.A]
                IT.TSX -> reg[RT.SP]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        reg[instr.regType] = r

        if (instr.insType != IT.TXS) {
            reg[FT.N] = testBit(r.toInt(), 7)
            reg[FT.Z] = r.toInt() == 0
        }
    }

    private fun opSET() {
        reg[instr.flagType] = true
    }

    private fun opCLEAR() {
        reg[instr.flagType] = false
    }

    private fun opBRANCH() {
        val offset = readSrc16(instr.addrMode)
        val f =
            when (instr.insType) {
                IT.BRSET -> reg[instr.flagType]
                IT.BRCLR -> !reg[instr.flagType]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }

        if (f) reg.setPC((reg.PC + offset).toUShort())
    }

    private fun opCOMPARE() {
        val r = reg[instr.regType]
        val m = readSrc8(instr.addrMode)
        val v = r - m
        reg[FT.C] = r >= m
        reg[FT.Z] = r == m
        reg[FT.N] = testBit(v.toInt(), 7)
    }

    private fun opLOGICAL() {
        val v =
            when (instr.insType) {
                IT.AND -> reg[RT.A] and readSrc8(instr.addrMode)
                IT.ORA -> reg[RT.A] or readSrc8(instr.addrMode)
                IT.EOR -> reg[RT.A] xor readSrc8(instr.addrMode)
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        reg[RT.A] = v
        reg[FT.N] = testBit(v.toInt(), 7)
        reg[FT.Z] = v.toInt() == 0
    }

    private fun opSHIFT() {
        val m = readSrc8(instr.addrMode).toUInt()
        val v: UInt

        when (instr.insType) {
            IT.ASL -> {
                v = m shl 1
                reg[FT.C] = testBit(m.toInt(), 7)
            }
            IT.LSR -> {
                v = m shr 1
                reg[FT.C] = testBit(m.toInt(), 0)
            }
            IT.ROL -> {
                val c = (if (reg[FT.C]) 1 else 0)
                v = (m shl 1) or c.toUInt()
                reg[FT.C] = testBit(m.toInt(), 7)
            }
            IT.ROR -> {
                val c = (if (reg[FT.C]) 1 else 0)
                v = (m shr 1) or (c.toUInt() shl 7)
                reg[FT.C] = testBit(m.toInt(), 0)
            }
            else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
        }

        when (instr.addrMode) {
            AM.ACCUMULATOR -> reg[RT.A] = v.toUByte()
            else -> {
                bus.writeByte(lastAddr, v.toUByte())
            }
        }

        reg[FT.N] = testBit(v.toInt(), 7)
        reg[FT.Z] = (v % 0x100u).toInt() == 0
    }

    private fun opPUSH() {
        var r = reg[instr.regType]
        if (instr.regType == RT.SR) {
            r = setBit(r.toInt(), reg.getFlagOrdinal(FT.B)).toUByte()
        }
        push(r)
    }

    private fun opPULL() {
        val r = pop()
        reg[instr.regType] = r

        when (instr.regType) {
            RT.SR -> {
                reg[FT.B] = false
                reg[FT.UNUSED2_IGN] = true
            }

            else -> {
                reg[FT.N] = testBit(r.toInt(), 7)
                reg[FT.Z] = r.toInt() == 0
            }
        }
    }

    private fun opINCREMENT() = indcr((readSrc8(instr.addrMode) + 1u).toUByte())

    private fun opDECREMENT() = indcr((readSrc8(instr.addrMode) - 1u).toUByte())

    private fun opNOP() = bus.dummyRead(reg.PC)

    private fun opJMP() = reg.setPC(readSrc16(instr.addrMode))

    private fun opJSR() {
        val v = (reg.PC + 1u).toUShort()
        push(MSB(v))
        push(LSB(v))
        reg.setPC(readSrc16(instr.addrMode))
    }

    private fun opADC() {
        val a = reg[RT.A]
        val m = readSrc8(instr.addrMode)
        val c = (if (reg[FT.C]) 1u else 0u)
        val v = (m + a + c).toInt()
        reg[RT.A] = v.toUByte()
        reg[FT.C] = v > 0xFF
        reg[FT.Z] = (v % 0x100) == 0
        reg[FT.N] = testBit(v, 7)
        reg.checkOverflow(a, m, v.toUByte())
    }

    private fun opSBC() {
        val a = reg[RT.A]
        val m = readSrc8(instr.addrMode)
        val c = (if (reg[FT.C]) 0u else 1u)
        val v = (a - m - c).toInt()
        reg[RT.A] = v.toUByte()
        reg[FT.C] = v >= 0
        reg[FT.Z] = (v % 0x100) == 0
        reg[FT.N] = testBit(v, 7)
        reg.checkOverflow(a, m, v.toUByte(), sbc = true)
    }

    private fun opBIT() {
        val a = reg[RT.A]
        val m = readSrc8(instr.addrMode)
        reg[FT.Z] = (a and m).toInt() == 0
        reg[FT.N] = testBit(m.toInt(), 7)
        reg[FT.V] = testBit(m.toInt(), 6)
    }

    private fun opBRK() {
        val v = (reg.PC + 1u).toUShort()
        push(MSB(v))
        push(LSB(v))
        reg.setPC(bus.readWord(0xFFFEu))
        push(setBit(reg[RT.SR].toInt(), FT.B.ordinal - 1).toUByte())
        reg[FT.I] = true
    }

    private fun opRTI() {
        reg[RT.SR] = pop()
        reg[FT.B] = false
        reg[FT.UNUSED2_IGN] = true
        val lo = pop()
        val hi = pop()
        reg.setPC(concat(hi, lo))
    }

    private fun opRTS() {
        val lo = pop()
        val hi = pop()
        reg.setPC((concat(hi, lo) + 1u).toUShort())
    }

    // ------------------------------------------------------------------------------------

    fun tick() {
        instr = INSTAB[fetch().toInt()]
        assert(instr.insType != IT.NONE) { "${instr.insType} is an illegal instruction type." }
        executors[instr.insType]?.invoke()
    }

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
            IT.AND to { opLOGICAL() },
            IT.EOR to { opLOGICAL() },
            IT.ORA to { opLOGICAL() },
            IT.ASL to { opSHIFT() },
            IT.LSR to { opSHIFT() },
            IT.ROL to { opSHIFT() },
            IT.ROR to { opSHIFT() },
            IT.COMPARE to { opCOMPARE() },
            IT.BIT to { opBIT() },
            IT.BRK to { opBRK() },
            IT.RTI to { opRTI() },
            IT.RTS to { opRTS() },
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
}
