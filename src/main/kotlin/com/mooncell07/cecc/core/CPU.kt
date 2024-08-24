package com.mooncell07.cecc.core

class CPU(
    private val bus: Bus,
) : Register() {
    var instr: INSTR = INSTAB[0xEA]
    private var lastAddr: UShort = 0x0000u
    private var pageCheck: Boolean = false

    // Fetchers
    // ------------------------------------------------------------------------------------

    private fun fetch(): UByte = bus.read(PC++)

    private fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    private fun handleInvalidAddress(
        base: UShort,
        effective: UShort,
    ) {
        pageCheck = MSB(base) != MSB(effective)
        if (pageCheck) {
            when (MSB(effective).toInt() - MSB(base).toInt()) {
                1, -255 -> bus.dummyRead((effective - 0x100u).toUShort())
                -1, 255 -> bus.dummyRead((effective + 0x100u).toUShort())
            }
        }
    }

    // ------------------------------------------------------------------------------------

    // Handlers for different addressing modes
    // ------------------------------------------------------------------------------------

    // Only works for instrs that use pre defined regs
    private fun getImpl(): UByte = this[instr.regType]

    private fun getImm(): UByte = fetch()

    private fun getAcc(): UByte = this[RT.A]

    private fun getAbs(): UShort = fetchWord()

    private fun getAbsX(): UShort {
        val base = fetchWord()
        val effective = (base + this[RT.X]).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun getAbsY(): UShort {
        val base = fetchWord()
        val effective = (base + this[RT.Y]).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun getZP(): UShort = fetch().toUShort()

    private fun getZPX(): UShort {
        val base = fetch()
        val v = ((base + this[RT.X]) % 0x100u).toUShort()
        bus.dummyRead(base.toUShort())
        return v
    }

    private fun getZPY(): UShort {
        val base = fetch()
        val v = ((base + this[RT.Y]) % 0x100u).toUShort()
        bus.dummyRead(base.toUShort())
        return v
    }

    private fun getRel(): UShort = fetch().toByte().toUShort()

    private fun getInd(): UShort {
        val base = fetchWord()
        val lo = bus.read(base)
        val hi =
            if (LSB(base).toUInt() == 0xFFu) {
                bus.read((base and 0xFF00u))
            } else {
                bus.read((base + 1u).toUShort())
            }
        return concat(hi, lo)
    }

    private fun getXInd(): UShort {
        val addr = fetch()
        bus.dummyRead(addr.toUShort())
        val base = (addr + this[RT.X]) % 0x100u
        val lo = bus.read(base.toUShort())
        val hiAddr = (base + 1u) % 0x100u
        val hi = bus.read(hiAddr.toUShort())
        return concat(hi, lo)
    }

    private fun getIndY(): UShort {
        val ptr = fetch()
        val lo = bus.read(ptr.toUShort())
        val hi = bus.read(((ptr + 1u) % 0x100u).toUShort())
        val base = concat(hi, lo)
        val effective = (base + this[RT.Y].toUShort()).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun readSource(mode: AddressingMode): UByte {
        if (mode == AM.INDIRECT || mode == AM.RELATIVE) {
            throw IllegalArgumentException("readSrc() does not support INDIRECT and RELATIVE modes.")
        }
        return when (mode) {
            AM.IMMEDIATE -> getImm()
            AM.ACCUMULATOR -> getAcc()
            AM.IMPLIED -> getImpl()
            else -> {
                lastAddr = decoders[mode]!!.invoke()
                bus.read(lastAddr)
            }
        }
    }

    private fun readSourceWord(mode: AddressingMode): UShort {
        val v = decoders[mode]!!.invoke()
        if (!pageCheck) {
            when (mode) {
                AM.INDIRECT_Y, AM.ABSOLUTE_Y, AM.ABSOLUTE_X -> bus.dummyRead(v)
                else -> {}
            }
        }
        return v
    }

    // ------------------------------------------------------------------------------------

    // Utils
    // ------------------------------------------------------------------------------------

    private fun push(
        data: UByte,
        dummy: Boolean = false,
    ) {
        bus.write((this[RT.SP] + 0x100u).toUShort(), data)
        if (!dummy) this[RT.SP]--
    }

    private fun pop(dummy: Boolean = false): UByte {
        if (!dummy) this[RT.SP]++
        val v = bus.read((this[RT.SP] + 0x100u).toUShort())
        return v
    }

    private fun indcr(incr: Boolean) {
        val m = readSource(instr.addrMode)
        val v = (if (incr) (m + 1u) else (m - 1u)).toUByte()
        when (instr.addrMode) {
            AM.IMPLIED -> {
                this[instr.regType] = v
                bus.dummyRead(PC)
            }
            else -> {
                if ((instr.addrMode == AM.ABSOLUTE_X) and !pageCheck) {
                    bus.dummyRead(lastAddr)
                }
                bus.dummyWrite(lastAddr, m)
                bus.write(lastAddr, v)
            }
        }
        this[FT.N] = testBit(v.toInt(), 7)
        this[FT.Z] = v.toInt() == 0
    }

    // ------------------------------------------------------------------------------------

    // Instruction Handlers
    // ------------------------------------------------------------------------------------

    private fun opLOAD() {
        val m = readSource(instr.addrMode)
        this[instr.regType] = m
        this[FT.N] = testBit(m.toInt(), 7)
        this[FT.Z] = m.toInt() == 0
    }

    private fun opSTORE() {
        bus.write(readSourceWord(instr.addrMode), this[instr.regType])
    }

    private fun opTRANSFER() {
        val r =
            when (instr.insType) {
                IT.TXA -> this[RT.X]
                IT.TYA -> this[RT.Y]
                IT.TXS -> this[RT.X]
                IT.TAY -> this[RT.A]
                IT.TAX -> this[RT.A]
                IT.TSX -> this[RT.SP]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        this[instr.regType] = r

        if (instr.insType != IT.TXS) {
            this[FT.N] = testBit(r.toInt(), 7)
            this[FT.Z] = r.toInt() == 0
        }

        bus.dummyRead(PC)
    }

    private fun opSET() {
        this[instr.flagType] = true
        bus.dummyRead(PC)
    }

    private fun opCLEAR() {
        this[instr.flagType] = false
        bus.dummyRead(PC)
    }

    private fun opBRANCH() {
        val offset = readSourceWord(instr.addrMode)
        val f =
            when (instr.insType) {
                IT.BRSET -> this[instr.flagType]
                IT.BRCLR -> !this[instr.flagType]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }

        if (f) {
            bus.dummyRead(PC)
            val effective = (PC + offset).toUShort()
            handleInvalidAddress(PC, effective)
            setPC(effective)
        }
    }

    private fun opCOMPARE() {
        val r = this[instr.regType]
        val m = readSource(instr.addrMode)
        val v = r - m
        this[FT.C] = r >= m
        this[FT.Z] = r == m
        this[FT.N] = testBit(v.toInt(), 7)
    }

    private fun opLOGICAL() {
        val v =
            when (instr.insType) {
                IT.AND -> this[RT.A] and readSource(instr.addrMode)
                IT.ORA -> this[RT.A] or readSource(instr.addrMode)
                IT.EOR -> this[RT.A] xor readSource(instr.addrMode)
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        this[RT.A] = v
        this[FT.N] = testBit(v.toInt(), 7)
        this[FT.Z] = v.toInt() == 0
    }

    private fun opSHIFT() {
        val m = readSource(instr.addrMode).toUInt()
        val v: UInt

        when (instr.insType) {
            IT.ASL -> {
                v = m shl 1
                this[FT.C] = testBit(m.toInt(), 7)
            }
            IT.LSR -> {
                v = m shr 1
                this[FT.C] = testBit(m.toInt(), 0)
            }
            IT.ROL -> {
                val c = (if (this[FT.C]) 1 else 0)
                v = (m shl 1) or c.toUInt()
                this[FT.C] = testBit(m.toInt(), 7)
            }
            IT.ROR -> {
                val c = (if (this[FT.C]) 1 else 0)
                v = (m shr 1) or (c.toUInt() shl 7)
                this[FT.C] = testBit(m.toInt(), 0)
            }
            else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
        }

        when (instr.addrMode) {
            AM.ACCUMULATOR -> {
                bus.dummyRead(PC)
                this[RT.A] = v.toUByte()
            }
            else -> {
                if ((instr.addrMode == AM.ABSOLUTE_X) and !pageCheck) {
                    bus.dummyRead(lastAddr)
                }
                bus.dummyWrite(lastAddr, m.toUByte())
                bus.write(lastAddr, v.toUByte())
            }
        }

        this[FT.N] = testBit(v.toInt(), 7)
        this[FT.Z] = (v % 0x100u).toInt() == 0
    }

    private fun opPUSH() {
        var r = this[instr.regType]
        if (instr.regType == RT.SR) {
            r = setBit(r.toInt(), getFlagOrdinal(FT.B)).toUByte()
        }
        bus.dummyRead(PC)
        push(r)
    }

    private fun opPULL() {
        bus.dummyRead(PC)
        pop(dummy = true)

        val r = pop()
        this[instr.regType] = r

        when (instr.regType) {
            RT.SR -> {
                this[FT.B] = false
                this[FT.UNUSED2_IGN] = true
            }

            else -> {
                this[FT.N] = testBit(r.toInt(), 7)
                this[FT.Z] = r.toInt() == 0
            }
        }
    }

    private fun opINCREMENT() = indcr(incr = true)

    private fun opDECREMENT() = indcr(incr = false)

    private fun opNOP() = bus.dummyRead(PC)

    private fun opJMP() = setPC(readSourceWord(instr.addrMode))

    private fun opJSR() {
        val lo = fetch()
        pop(dummy = true)
        push(MSB(PC))
        push(LSB(PC))
        val hi = bus.read(PC)
        setPC(concat(hi, lo))
    }

    private fun opADC() {
        val a = this[RT.A]
        val m = readSource(instr.addrMode)
        val c = (if (this[FT.C]) 1u else 0u)
        val v = (m + a + c).toInt()
        this[RT.A] = v.toUByte()
        this[FT.C] = v > 0xFF
        this[FT.Z] = (v % 0x100) == 0
        this[FT.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte())
    }

    private fun opSBC() {
        val a = this[RT.A]
        val m = readSource(instr.addrMode)
        val c = (if (this[FT.C]) 0u else 1u)
        val v = (a - m - c).toInt()
        this[RT.A] = v.toUByte()
        this[FT.C] = v >= 0
        this[FT.Z] = (v % 0x100) == 0
        this[FT.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte(), sbc = true)
    }

    private fun opBIT() {
        val a = this[RT.A]
        val m = readSource(instr.addrMode)
        this[FT.Z] = (a and m).toInt() == 0
        this[FT.N] = testBit(m.toInt(), 7)
        this[FT.V] = testBit(m.toInt(), 6)
    }

    private fun opBRK() {
        bus.dummyRead(PC)

        val v = (PC + 1u).toUShort()
        push(MSB(v))
        push(LSB(v))
        push(setBit(this[RT.SR].toInt(), FT.B.ordinal - 1).toUByte())
        setPC(bus.readWord(0xFFFEu))

        this[FT.I] = true
    }

    private fun opRTI() {
        bus.dummyRead(PC)
        pop(dummy = true)

        this[RT.SR] = pop()
        val lo = pop()
        val hi = pop()

        this[FT.B] = false
        this[FT.UNUSED2_IGN] = true

        setPC(concat(hi, lo))
    }

    private fun opRTS() {
        bus.dummyRead(PC)
        pop(dummy = true)

        val lo = pop()
        val hi = pop()

        setPC(concat(hi, lo))
        bus.dummyRead(PC++)
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
