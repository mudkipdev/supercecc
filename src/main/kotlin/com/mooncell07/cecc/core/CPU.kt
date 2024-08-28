package com.mooncell07.cecc.core

class CPU(
    private val bus: Bus,
) : Register() {
    private var instruction: Instruction = instructionTable[0xEA]
    private var lastAddr: UShort = 0x0000u
    private var pageCheck: Boolean = false

    // Fetchers
    ///////////////////////////////////////////////////////////////////////////////////////

    private fun fetch(): UByte = bus.read(pc++)

    private fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()
        return concat(hi, lo)
    }

    private fun handleInvalidAddress(base: UShort, effective: UShort) {
        pageCheck = mostSignificantByte(base) != mostSignificantByte(effective)

        if (pageCheck) {
            when (mostSignificantByte(effective).toInt() - mostSignificantByte(base).toInt()) {
                1, -255 -> bus.dummyRead((effective - 0x100u).toUShort())
                -1, 255 -> bus.dummyRead((effective + 0x100u).toUShort())
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    // Handlers for different addressing modes
    ///////////////////////////////////////////////////////////////////////////////////////

    // Only works for instructions that use pre defined registers
    private fun getIMPL(): UByte = this[instruction.registerType]

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
            if (leastSignificantByte(base).toUInt() == 0xFFu) {
                bus.read((base and 0xFF00u))
            } else {
                bus.read((base + 1u).toUShort())
            }
        return concat(hi, lo)
    }

    private fun getXIND(): UShort {
        val address = fetch()
        bus.dummyRead(address.toUShort())
        val base = (address + this[RegisterType.X]) % 0x100u
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

    ///////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////

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
        val m = readSource(instruction.addressingMode)
        val v = (if (incr) (m + 1u) else (m - 1u)).toUByte()

        when (instruction.addressingMode) {
            AddressingMode.IMPLIED -> {
                this[instruction.registerType] = v
                bus.dummyRead(pc)
            }

            else -> {
                if ((instruction.addressingMode == AddressingMode.ABSOLUTE_X) and !pageCheck) {
                    bus.dummyRead(lastAddr)
                }
                bus.dummyWrite(lastAddr, m)
                bus.write(lastAddr, v)
            }
        }

        this[FlagType.N] = testBit(v.toInt(), 7)
        this[FlagType.Z] = v.toInt() == 0
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // Instruction Handlers
    ///////////////////////////////////////////////////////////////////////////////////////

    private fun operationLoad() {
        val m = readSource(instruction.addressingMode)
        this[instruction.registerType] = m
        this[FlagType.N] = testBit(m.toInt(), 7)
        this[FlagType.Z] = m.toInt() == 0
    }

    private fun operationStore() {
        bus.write(readSourceWord(instruction.addressingMode), this[instruction.registerType])
    }

    private fun operationTransfer() {
        val r =
            when (instruction.instructionType) {
                InstructionType.TXA -> this[RegisterType.X]
                InstructionType.TYA -> this[RegisterType.Y]
                InstructionType.TXS -> this[RegisterType.X]
                InstructionType.TAY -> this[RegisterType.A]
                InstructionType.TAX -> this[RegisterType.A]
                InstructionType.TSX -> this[RegisterType.SP]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instruction.instructionType}")
            }
        this[instruction.registerType] = r

        if (instruction.instructionType != InstructionType.TXS) {
            this[FlagType.N] = testBit(r.toInt(), 7)
            this[FlagType.Z] = r.toInt() == 0
        }

        bus.dummyRead(pc)
    }

    private fun operationSet() {
        this[instruction.flagType] = true
        bus.dummyRead(pc)
    }

    private fun operationClear() {
        this[instruction.flagType] = false
        bus.dummyRead(pc)
    }

    private fun operationBranch() {
        val offset = readSourceWord(instruction.addressingMode)
        val f =
            when (instruction.instructionType) {
                InstructionType.BRSET -> this[instruction.flagType]
                InstructionType.BRCLR -> !this[instruction.flagType]
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instruction.instructionType}")
            }

        if (f) {
            bus.dummyRead(pc)
            val effective = (pc + offset).toUShort()
            handleInvalidAddress(pc, effective)
            pc = effective
        }
    }

    private fun opCOMPARE() {
        val r = this[instruction.registerType]
        val m = readSource(instruction.addressingMode)
        val v = r - m
        this[FlagType.C] = r >= m
        this[FlagType.Z] = r == m
        this[FlagType.N] = testBit(v.toInt(), 7)
    }

    private fun opLOGICAL() {
        val v =
            when (instruction.instructionType) {
                InstructionType.AND -> this[RegisterType.A] and readSource(instruction.addressingMode)
                InstructionType.ORA -> this[RegisterType.A] or readSource(instruction.addressingMode)
                InstructionType.EOR -> this[RegisterType.A] xor readSource(instruction.addressingMode)
                else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instruction.instructionType}")
            }
        this[RegisterType.A] = v
        this[FlagType.N] = testBit(v.toInt(), 7)
        this[FlagType.Z] = v.toInt() == 0
    }

    private fun opSHIFT() {
        val m = readSource(instruction.addressingMode).toUInt()
        val v: UInt

        when (instruction.instructionType) {
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
            else -> throw IllegalArgumentException("Unsupported Instruction Type: ${instruction.instructionType}")
        }

        when (instruction.addressingMode) {
            AddressingMode.ACCUMULATOR -> {
                bus.dummyRead(pc)
                this[RegisterType.A] = v.toUByte()
            }
            else -> {
                if ((instruction.addressingMode == AddressingMode.ABSOLUTE_X) and !pageCheck) {
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
        var r = this[instruction.registerType]
        if (instruction.registerType == RegisterType.SR) {
            r = setBit(r.toInt(), getFlagOrdinal(FlagType.B)).toUByte()
        }
        bus.dummyRead(pc)
        push(r)
    }

    private fun opPULL() {
        bus.dummyRead(pc)
        pop(dummy = true)

        val r = pop()
        this[instruction.registerType] = r

        when (instruction.registerType) {
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

    private fun opNOP() = bus.dummyRead(pc)

    private fun opJMP() {
        pc = readSourceWord(instruction.addressingMode)
    }

    private fun opJSR() {
        val lo = fetch()
        pop(dummy = true)
        push(mostSignificantByte(pc))
        push(leastSignificantByte(pc))
        val hi = bus.read(pc)
        pc = concat(hi, lo)
    }

    private fun opADC() {
        val a = this[RegisterType.A]
        val m = readSource(instruction.addressingMode)
        val c = (if (this[FlagType.C]) 1u else 0u)
        val v = (m + a + c).toInt()
        this[RegisterType.A] = v.toUByte()
        this[FlagType.C] = v > 0xFF
        this[FlagType.Z] = (v % 0x100) == 0
        this[FlagType.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte())
    }

    private fun operationSbc() {
        val a = this[RegisterType.A]
        val m = readSource(instruction.addressingMode)
        val c = (if (this[FlagType.C]) 0u else 1u)
        val v = (a - m - c).toInt()
        this[RegisterType.A] = v.toUByte()
        this[FlagType.C] = v >= 0
        this[FlagType.Z] = (v % 0x100) == 0
        this[FlagType.N] = testBit(v, 7)
        checkOverflow(a, m, v.toUByte(), sbc = true)
    }

    private fun operationBit() {
        val a = this[RegisterType.A]
        val m = readSource(instruction.addressingMode)
        this[FlagType.Z] = (a and m).toInt() == 0
        this[FlagType.N] = testBit(m.toInt(), 7)
        this[FlagType.V] = testBit(m.toInt(), 6)
    }

    private fun operationBrk() {
        bus.dummyRead(pc)

        val v = (pc + 1u).toUShort()
        push(mostSignificantByte(v))
        push(leastSignificantByte(v))
        push(setBit(this[RegisterType.SR].toInt(), FlagType.B.ordinal - 1).toUByte())
        pc = bus.readWord(0xFFFEu)

        this[FlagType.I] = true
    }

    private fun operationRti() {
        bus.dummyRead(pc)
        pop(dummy = true)

        this[RegisterType.SR] = pop()
        val lo = pop()
        val hi = pop()

        this[FlagType.B] = false
        this[FlagType.UNUSED2_IGN] = true

        pc = concat(hi, lo)
    }

    private fun operationRts() {
        bus.dummyRead(pc)
        pop(dummy = true)

        val lo = pop()
        val hi = pop()

        pc = concat(hi, lo)
        bus.dummyRead(pc++)
    }

    ///////////////////////////////////////////////////////////////////////////////////////

    fun tick() {
        instruction = instructionTable[fetch().toInt()]
        assert(instruction.instructionType != InstructionType.NONE) { "${instruction.instructionType} is an illegal instruction type." }
        executors[instruction.instructionType]?.invoke()
    }

    private val executors: Map<InstructionType, () -> Unit> = mapOf(
        InstructionType.LOAD to { operationLoad() },
        InstructionType.STORE to { operationStore() },
        InstructionType.JMP to { opJMP() },
        InstructionType.JSR to { opJSR() },
        InstructionType.NOP to { opNOP() },
        InstructionType.SET to { operationSet() },
        InstructionType.BRCLR to { operationBranch() },
        InstructionType.BRSET to { operationBranch() },
        InstructionType.CLEAR to { operationClear() },
        InstructionType.TAX to { operationTransfer() },
        InstructionType.TAY to { operationTransfer() },
        InstructionType.TSX to { operationTransfer() },
        InstructionType.TXA to { operationTransfer() },
        InstructionType.TYA to { operationTransfer() },
        InstructionType.TXS to { operationTransfer() },
        InstructionType.PUSH to { opPUSH() },
        InstructionType.PULL to { opPULL() },
        InstructionType.INCREMENT to { opINCREMENT() },
        InstructionType.DECREMENT to { opDECREMENT() },
        InstructionType.ADC to { opADC() },
        InstructionType.SBC to { operationSbc() },
        InstructionType.AND to { opLOGICAL() },
        InstructionType.EOR to { opLOGICAL() },
        InstructionType.ORA to { opLOGICAL() },
        InstructionType.ASL to { opSHIFT() },
        InstructionType.LSR to { opSHIFT() },
        InstructionType.ROL to { opSHIFT() },
        InstructionType.ROR to { opSHIFT() },
        InstructionType.COMPARE to { opCOMPARE() },
        InstructionType.BIT to { operationBit() },
        InstructionType.BRK to { operationBrk() },
        InstructionType.RTI to { operationRti() },
        InstructionType.RTS to { operationRts() }
    )

    private val decoders: Map<AddressingMode, () -> UShort> = mapOf(
        AddressingMode.ABSOLUTE to { getABS() },
        AddressingMode.ABSOLUTE_X to { getABSX() },
        AddressingMode.ABSOLUTE_Y to { getABSY() },
        AddressingMode.X_INDIRECT to { getXIND() },
        AddressingMode.INDIRECT_Y to { getINDY() },
        AddressingMode.ZEROPAGE to { getZP() },
        AddressingMode.ZEROPAGE_X to { getZPX() },
        AddressingMode.ZEROPAGE_Y to { getZPY() },
        AddressingMode.RELATIVE to { getREL() },
        AddressingMode.INDIRECT to { getIND() }
    )
}
