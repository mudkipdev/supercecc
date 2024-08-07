package com.mooncell07.cecc.core

class Bus(
    private val area: UByteArray = UByteArray(0x10000) { 0u },
) {
    var cycles: MutableList<MutableList<Any>> = mutableListOf(mutableListOf(Any()))

    fun readByte(address: UShort): UByte {
        val res = area[address.toInt()]
        cycles.add(mutableListOf(address.toDouble(), res.toDouble(), "read"))
        return res
    }

    fun writeByte(
        address: UShort,
        data: UByte,
    ) {
        area[address.toInt()] = data
        cycles.add(mutableListOf(address.toDouble(), data.toDouble(), "write"))
    }

    fun readWord(
        address: UShort,
        wrapping: Boolean = false,
    ): UShort {
        val lo = readByte(address)
        var hiAddr = (address + 1u)
        if (wrapping) {
            hiAddr %= 0x100u
        }
        val hi = readByte(hiAddr.toUShort())
        return concat(hi, lo)
    }
}
