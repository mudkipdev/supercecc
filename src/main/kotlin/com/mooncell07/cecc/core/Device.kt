package com.mooncell07.cecc.core

abstract class AbstractDevice {
    abstract val type: DeviceType
    abstract val size: Int
    abstract val base: Int

    abstract fun read(address: UShort): UByte

    abstract fun write(
        address: UShort,
        data: UByte,
    )
}

open class Device : AbstractDevice() {
    override val type = DT.EMPTY
    override val size = -1
    override val base = -1
    open val area: UByteArray = ubyteArrayOf()

    override fun write(
        address: UShort,
        data: UByte,
    ) {
        area[(address - base.toUShort()).toInt()] = data
    }

    override fun read(address: UShort): UByte {
        val a = area[(address - base.toUShort()).toInt()]
        return a
    }
}
