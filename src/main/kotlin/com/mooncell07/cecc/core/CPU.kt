package com.mooncell07.cecc.core

open class Stream(
    val reg: Register,
    val bus: Bus,
) {
    fun fetch(): UByte = bus.readByte(reg.PC++)

    private fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    fun read(mode: AddressingMode): UByte =
        when (mode) {
            AM.IMMEDIATE -> fetch()
            AM.ACCUMULATOR -> reg[RT.A]
            AM.ABSOLUTE -> bus.readByte(fetchWord())
            AM.ABSOLUTE_X -> bus.readByte((fetchWord() + reg[RT.X]).toUShort())
            AM.ABSOLUTE_Y -> bus.readByte((fetchWord() + reg[RT.Y]).toUShort())

            AM.X_INDIRECT -> {
                val base = (fetch() + reg[RT.X]).toUByte()
                val ptrAddr = concat((base + 1u).toUByte(), base)
                val ptr = bus.readWord(ptrAddr)
                bus.readByte(ptr)
            }

            AM.INDIRECT_Y -> {
                val base = fetch()
                val ptrAddr = concat((base + 1u).toUByte(), base) + reg[RT.Y]
                val ptr = bus.readWord(ptrAddr.toUShort())
                bus.readByte(ptr)
            }

            AM.ZEROPAGE -> bus.readByte(fetch().toUShort())

            AM.ZEROPAGE_X -> {
                val fullAddr = bus.readWord((fetch() + reg[RT.X]).toUShort())
                bus.readByte(fullAddr)
            }

            AM.ZEROPAGE_Y -> {
                val fullAddr = bus.readWord((fetch() + reg[RT.Y]).toUShort())
                bus.readByte(fullAddr)
            }

            else -> 0xFFu
        }

    fun readIndirect(): UShort = bus.readWord(fetchWord())

    fun readRelative(): Byte = fetch().toByte()
}

class CPU(
    reg: Register,
    bus: Bus,
) : Stream(reg, bus) {
    var opcode: Int = 0x00
    var instr: INSTR? = INSTAB[0xEA]
    val handlers: Array<INSTR?> = arrayOfNulls(256)

    private fun opLD(instr: INSTR) {
        reg[regType] = data
    }

    private fun opST(
        regType: RT,
        address: UShort,
    ) {
        bus.writeByte(address, reg[regType])
    }

    private fun opTrans(
        srcReg: RT,
        destReg: RT,
    ) {
        reg[destReg] = reg[srcReg]
    }
}
