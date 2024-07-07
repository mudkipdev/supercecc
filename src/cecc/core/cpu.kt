package cecc.core

typealias R = RegType
typealias F = FlagType
typealias AM = AddressingMode

open class Stream(private val reg: Register, private val bus: Bus) {
    var mode: AddressingMode? = null

    fun fetch(): UByte {
        return bus.readByte(reg.PC++)
    }

    fun fetchWord(): UShort {
        val lo: UByte = fetch()
        val hi: UByte = fetch()

        return concat(hi, lo)
    }

    fun read(): UByte {
        return when (mode) {
            AM.IMMEDIATE -> fetch()
            AM.ACCUMULATOR -> reg[R.A]
            AM.INDIRECT -> bus.readByte(fetchWord())

            AM.X_INDIRECT -> {
                val fullAddr = bus.readWord((fetch() + reg[R.X]).toUShort())
                bus.readByte(fullAddr)
            }

            AM.INDIRECT_Y -> {
                val fullAddr = bus.readWord(fetch().toUShort()) + reg[R.Y]
                bus.readByte(fullAddr.toUShort())
            }

            else -> 0xFFu
        }
    }
}

class CPU(reg: Register, bus: Bus) : Stream(reg, bus) {
    var opcode: UByte = 0x00u
}
