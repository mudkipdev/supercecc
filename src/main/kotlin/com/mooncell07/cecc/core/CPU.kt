package com.mooncell07.cecc.core

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
            AM.ABSOLUTE -> bus.readByte(fetchWord())
            AM.ABSOLUTE_X -> bus.readByte((fetchWord() + reg[R.X]).toUShort())
            AM.ABSOLUTE_Y -> bus.readByte((fetchWord() + reg[R.Y]).toUShort())

            AM.X_INDIRECT -> {
                val base = (fetch() + reg[R.X]).toUByte()
                val ptrAddr = concat((base + 1u).toUByte(), base)
                val ptr = bus.readWord(ptrAddr)
                bus.readByte(ptr)
            }

            AM.INDIRECT_Y -> {
                val base = fetch()
                val ptrAddr = concat((base + 1u).toUByte(), base) + reg[R.Y]
                val ptr = bus.readWord(ptrAddr.toUShort())
                bus.readByte(ptr)
            }

            AM.ZEROPAGE -> bus.readByte(fetch().toUShort())

            AM.ZEROPAGE_X -> {
                val fullAddr = bus.readWord((fetch() + reg[R.X]).toUShort())
                bus.readByte(fullAddr)
            }

            AM.ZEROPAGE_Y -> {
                val fullAddr = bus.readWord((fetch() + reg[R.Y]).toUShort())
                bus.readByte(fullAddr)
            }

            else -> 0xFFu
        }
    }

    fun readIndirect(): UShort {
        return bus.readWord(fetchWord())
    }

    fun readRelative(): Byte {
        return fetch().toByte()
    }
}

class CPU(reg: Register, bus: Bus) : Stream(reg, bus) {
    var opcode: UByte = 0x00u
}
