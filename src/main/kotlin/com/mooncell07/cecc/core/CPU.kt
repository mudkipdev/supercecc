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
    private fun getIMPL(): UByte = this[instr.registerType]

    private fun getIMM(): UByte = fetch()

    private fun getACC(): UByte = this[RegisterType.A]

    private fun getABS(): UShort = fetchWord()

    private fun getABSX(): UShort {
        val base = fetchWord()
        val effective = (base + this[RegisterType.X]).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun getABSY(): UShort {
        val base = fetchWord()
        val effective = (base + this[RegisterType.Y]).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun getZP(): UShort = fetch().toUShort()

    private fun getZPX(): UShort {
        val base = fetch()
        val v = ((base + this[RegisterType.X]) % 0x100u).toUShort()
        bus.dummyRead(base.toUShort())
        return v
    }

    private fun getZPY(): UShort {
        val base = fetch()
        val v = ((base + this[RegisterType.Y]) % 0x100u).toUShort()
        bus.dummyRead(base.toUShort())
        return v
    }

    private fun getREL(): UShort = fetch().toByte().toUShort()

    private fun getIND(): UShort {
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

    private fun getXIND(): UShort {
        val addr = fetch()
        bus.dummyRead(addr.toUShort())
        val base = (addr + this[RegisterType.X]) % 0x100u
        val lo = bus.read(base.toUShort())
        val hiAddr = (base + 1u) % 0x100u
        val hi = bus.read(hiAddr.toUShort())
        return concat(hi, lo)
    }

    private fun getINDY(): UShort {
        val ptr = fetch()
        val lo = bus.read(ptr.toUShort())
        val hi = bus.read(((ptr + 1u) % 0x100u).toUShort())
        val base = concat(hi, lo)
        val effective = (base + this[RegisterType.Y].toUShort()).toUShort()
        handleInvalidAddress(base, effective)
        return effective
    }

    private fun readSource(mode: AddressingMode): UByte {
        if (mode == AddressingMode.INDIRECT || mode == AddressingMode.RELATIVE) {
            throw IllegalArgumentException("readSrc() does not support INDIRECT and RELATIVE modes.")
        }
        return when (mode) {
            AddressingMode.IMMEDIATE -> getIMM()
            AddressingMode.ACCUMULATOR -> getACC()
            AddressingMode.IMPLIED -> getIMPL()
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
                AddressingMode.INDIRECT_Y, AddressingMode.ABSOLUTE_Y, AddressingMode.ABSOLUTE_X -> bus.dummyRead(v)
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
        bus.write((this[RegisterType.SP] + 0x100u).toUShort(), data)
        if (!dummy) this[RegisterType.SP]--
    }

    private fun pop(dummy: Boolean = false): UByte {
        if (!dummy) this[RegisterType.SP]++
        val v = bus.read((this[RegisterType.SP] + 0x100u).toUShort())
        return v
    }

    private fun indcr(incr: Boolean) {
        val m = readSource(instr.addrMode)
        val v = (if (incr) (m + 1u) else (m - 1u)).toUByte()
        when (instr.addrMode) {
            AddressingMode.IMPLIED -> {
                this[instr.registerType] = v
                bus.dummyRead(PC)
            }
            else -> {
                if ((instr.addrMode == AddressingMode.ABSOLUTE_X) and !pageCheck) {
                    bus.dummyRead(lastAddr)
                }
                bus.dummyWrite(lastAddr, m)
                bus.write(lastAddr, v)
            }
        }
        this[FlagType.N] = testBit(v.toInt(), 7)
        this[FlagType.Z] = v.toInt() == 0
    }

    // ------------------------------------------------------------------------------------

    // Instruction Handlers
    // ------------------------------------------------------------------------------------

    private fun opLOAD() {
        val m = readSource(instr.addrMode)
        this[instr.registerType] = m
        this[FlagType.N] = testBit(m.toInt(), 7)
        this[FlagType.Z] = m.toInt() == 0
    }

    private fun opSTORE() {
        bus.write(readSourceWord(instr.addrMode), this[instr.registerType])
    }

    private fun opTRANSFER() {
        val r =
            when (instr.insType) {
                InstructionType.TXA -> this[RegisterType.X]
                InstructionType.TYA -> this[RegisterType.Y]
                InstructionType.TXS -> this[RegisterType.X]
                InstructionType.TAY -> this[RegisterType.A]
                InstructionType.TAX -> this[RegisterType.A]
                InstructionType.TSX -> this[RegisterType.SP]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        this[instr.registerType] = r

        if (instr.insType != InstructionType.TXS) {
            this[FlagType.N] = testBit(r.toInt(), 7)
            this[FlagType.Z] = r.toInt() == 0
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
                InstructionType.BRSET -> this[instr.flagType]
                InstructionType.BRCLR -> !this[instr.flagType]
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
        val r = this[instr.registerType]
        val m = readSource(instr.addrMode)
        val v = r - m
        this[FlagType.C] = r >= m
        this[FlagType.Z] = r == m
        this[FlagType.N] = testBit(v.toInt(), 7)
    }

    private fun opLOGICAL() {
        val v =
            when (instr.insType) {
                InstructionType.AND -> this[RegisterType.A] and readSource(instr.addrMode)
                InstructionType.ORA -> this[RegisterType.A] or readSource(instr.addrMode)
                InstructionType.EOR -> this[RegisterType.A] xor readSource(instr.addrMode)
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
            }
        this[RegisterType.A] = v
        this[FlagType.N] = testBit(v.toInt(), 7)
        this[FlagType.Z] = v.toInt() == 0
    }

    private fun opSHIFT() {
        val m = readSource(instr.addrMode).toUInt()
        val v: UInt

        when (instr.insType) {
            InstructionType.ASL -> {
                v = m shl 1
                this[FlagType.C] = testBit(m.toInt(), 7)
            }
            InstructionType.LSR -> {
                v = m shr 1
                this[FlagType.C] = testBit(m.toInt(), 0)
            }
            InstructionType.ROL -> {
                val c = (if (this[FlagType.C]) 1 else 0)
                v = (m shl 1) or c.toUInt()
                this[FlagType.C] = testBit(m.toInt(), 7)
            }
            InstructionType.ROR -> {
                val c = (if (this[FlagType.C]) 1 else 0)
                v = (m shr 1) or (c.toUInt() shl 7)
                this[FlagType.C] = testBit(m.toInt(), 0)
            }
            else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instr.insType}")
        }

        when (instr.addrMode) {
            AddressingMode.ACCUMULATOR -> {
                bus.dummyRead(PC)
                this[RegisterType.A] = v.toUByte()
            }
            else -> {
                if ((instr.addrMode == AddressingMode.ABSOLUTE_X) and !pageCheck) {
                    bus.dummyRead(lastAddr)
                }
                bus.dummyWrite(lastAddr, m.toUByte())
                bus.write(lastAddr, v.toUByte())
            }
        }

        this[FlagType.N] = testBit(v.toInt(), 7)
        this[FlagType.Z] = (v % 0x100u).toInt() == 0
    }

    private fun opPUSH() {
        var r = this[instr.registerType]
        if (instr.registerType == RegisterType.SR) {
            r = setBit(r.toInt(), getFlagOrdinal(FlagType.B)).toUByte()
        }
        bus.dummyRead(PC)
        push(r)
    }

    private fun opPULL() {
        bus.dummyRead(PC)
        pop(dummy = true)

        val r = pop()
        this[instr.registerType] = r

        when (instr.registerType) {
            RegisterType.SR -> {
                this[FlagType.B] = false
                this[FlagType.UNUSED2_IGN] = true
            }

            else -> {
                this[FlagType.N] = testBit(r.toInt(), 7)
                this[FlagType.Z] = r.toInt() == 0
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
        val a = this[RegisterType.A]
        val m = readSource(instr.addrMode)
        val c = (if (this[FlagType.C]) 1u else 0u)
        val v = (m + a + c).toInt()
        this[RegisterType.A] = v.toUByte()
        this[FlagType.C] = v > 0xFF
        this[FlagType.Z] = (v % 0x100) == 0
        this[FlagType.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte())
    }

    private fun opSBC() {
        val a = this[RegisterType.A]
        val m = readSource(instr.addrMode)
        val c = (if (this[FlagType.C]) 0u else 1u)
        val v = (a - m - c).toInt()
        this[RegisterType.A] = v.toUByte()
        this[FlagType.C] = v >= 0
        this[FlagType.Z] = (v % 0x100) == 0
        this[FlagType.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte(), sbc = true)
    }

    private fun opBIT() {
        val a = this[RegisterType.A]
        val m = readSource(instr.addrMode)
        this[FlagType.Z] = (a and m).toInt() == 0
        this[FlagType.N] = testBit(m.toInt(), 7)
        this[FlagType.V] = testBit(m.toInt(), 6)
    }

    private fun opBRK() {
        bus.dummyRead(PC)

        val v = (PC + 1u).toUShort()
        push(MSB(v))
        push(LSB(v))
        push(setBit(this[RegisterType.SR].toInt(), FlagType.B.ordinal - 1).toUByte())
        setPC(bus.readWord(0xFFFEu))

        this[FlagType.I] = true
    }

    private fun opRTI() {
        bus.dummyRead(PC)
        pop(dummy = true)

        this[RegisterType.SR] = pop()
        val lo = pop()
        val hi = pop()

        this[FlagType.B] = false
        this[FlagType.UNUSED2_IGN] = true

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
        assert(instr.insType != InstructionType.NONE) { "${instr.insType} is an illegal instruction type." }
        executors[instr.insType]?.invoke()
    }

    private val executors: Map<InstructionType, () -> Unit> =
        mapOf(
            InstructionType.LOAD to { opLOAD() },
            InstructionType.STORE to { opSTORE() },
            InstructionType.JMP to { opJMP() },
            InstructionType.JSR to { opJSR() },
            InstructionType.NOP to { opNOP() },
            InstructionType.SET to { opSET() },
            InstructionType.BRCLR to { opBRANCH() },
            InstructionType.BRSET to { opBRANCH() },
            InstructionType.CLEAR to { opCLEAR() },
            InstructionType.TAX to { opTRANSFER() },
            InstructionType.TAY to { opTRANSFER() },
            InstructionType.TSX to { opTRANSFER() },
            InstructionType.TXA to { opTRANSFER() },
            InstructionType.TYA to { opTRANSFER() },
            InstructionType.TXS to { opTRANSFER() },
            InstructionType.PUSH to { opPUSH() },
            InstructionType.PULL to { opPULL() },
            InstructionType.INCREMENT to { opINCREMENT() },
            InstructionType.DECREMENT to { opDECREMENT() },
            InstructionType.ADC to { opADC() },
            InstructionType.SBC to { opSBC() },
            InstructionType.AND to { opLOGICAL() },
            InstructionType.EOR to { opLOGICAL() },
            InstructionType.ORA to { opLOGICAL() },
            InstructionType.ASL to { opSHIFT() },
            InstructionType.LSR to { opSHIFT() },
            InstructionType.ROL to { opSHIFT() },
            InstructionType.ROR to { opSHIFT() },
            InstructionType.COMPARE to { opCOMPARE() },
            InstructionType.BIT to { opBIT() },
            InstructionType.BRK to { opBRK() },
            InstructionType.RTI to { opRTI() },
            InstructionType.RTS to { opRTS() },
        )

    private val decoders: Map<AddressingMode, () -> UShort> =
        mapOf(
            AddressingMode.ABSOLUTE to { getABS() },
            AddressingMode.ABSOLUTE_X to { getABSX() },
            AddressingMode.ABSOLUTE_Y to { getABSY() },
            AddressingMode.X_INDIRECT to { getXIND() },
            AddressingMode.INDIRECT_Y to { getINDY() },
            AddressingMode.ZEROPAGE to { getZP() },
            AddressingMode.ZEROPAGE_X to { getZPX() },
            AddressingMode.ZEROPAGE_Y to { getZPY() },
            AddressingMode.RELATIVE to { getREL() },
            AddressingMode.INDIRECT to { getIND() },
        )
}
