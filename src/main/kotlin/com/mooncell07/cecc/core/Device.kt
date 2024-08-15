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

class Device : AbstractDevice() {
    override val type = DT.EMPTY
    override val size = -1
    override val base = -1

    override fun write(
        address: UShort,
        data: UByte,
    ): Unit = throw IllegalAccessError("Empty object doesn't support writing to.")

    override fun read(address: UShort): UByte = throw IllegalAccessError("Empty object doesn't support reading from.")
}
