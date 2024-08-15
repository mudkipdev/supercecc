package com.mooncell07.cecc.core

class Bus(
    private vararg val deviceMap: AbstractDevice,
) : AbstractDevice() {
    override val type = DT.BUS
    override val size = 0xFFFF
    override val base = 0

    init {
        deviceMap.sortBy { it.base }
    }

    override fun read(address: UShort): UByte = deviceMap.find { address.toInt() <= (it.base + it.size) }!!.read(address)

    override fun write(
        address: UShort,
        data: UByte,
    ) = deviceMap.find { address.toInt() <= (it.base + it.size) }!!.write(address, data)

    fun readWord(address: UShort): UShort {
        val lo = read(address)
        val hi = read((address + 1u).toUShort())
        return concat(hi, lo)
    }

    fun dummyRead(address: UShort) = read(address)

    fun dummyWrite(
        address: UShort,
        data: UByte,
    ) = write(address, data)
}
