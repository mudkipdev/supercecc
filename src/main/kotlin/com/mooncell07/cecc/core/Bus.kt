package com.mooncell07.cecc.core

class Bus(
    private val area: UByteArray = UByteArray(0x10000) { 0u },
) {
    var cycles: MutableList<MutableList<Any>> = mutableListOf(mutableListOf(Any()))

    fun readByte(
        address: UShort,
        ext: Boolean = false,
    ): UByte {
        val res = area[address.toInt()]
        if (!ext) {
            cycles.add(mutableListOf(address.toDouble(), res.toDouble(), "read"))
        }
        return res
    }

    fun writeByte(
        address: UShort,
        data: UByte,
        ext: Boolean = false,
    ) {
        area[address.toInt()] = data
        if (!ext) {
            cycles.add(mutableListOf(address.toDouble(), data.toDouble(), "write"))
        }
    }

    fun readWord(address: UShort): UShort {
        val lo = readByte(address)
        val hi = readByte((address + 1u).toUShort())
        return concat(hi, lo)
    }

    fun dummyRead(address: UShort) = readByte(address)

    fun dummyWrite(
        address: UShort,
        data: UByte,
    ) = writeByte(address, data)
}
