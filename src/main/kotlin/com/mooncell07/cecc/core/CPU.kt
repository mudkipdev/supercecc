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

    fun readSrc(mode: AddressingMode): UByte =
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

    fun readSrcInd(): UShort = bus.readWord(fetchWord())

    fun readSrcRel(): Byte = fetch().toByte()

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
            )
    }

    private fun opLOAD() {
        writeDst(instr.regType, readSrc(instr.addrMode))
    }

    private fun opSTORE() {
        bus.writeByte(readSrc(instr.addrMode).toUShort(), reg[instr.regType])
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun tick() {
        val op = fetch().toInt()
        instr = INSTAB[op]
        println(instr)
        println(reg.PC.toHexString())
        handlers[instr.insType]?.invoke()
    }
}
