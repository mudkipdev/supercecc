package com.mooncell07.cecc.core.graphics

import com.mooncell07.cecc.core.AbstractDevice
import com.mooncell07.cecc.core.Cartridge
import com.mooncell07.cecc.core.DeviceType
import com.mooncell07.cecc.core.Device

class CHRROM(
    private val cart: Cartridge,
) : Device() {
    override val type = DeviceType.CHRROM
    override val size = 0x1FFF
    override val base = 0x0000
    override val area: UByteArray = UByteArray(size + 1)

    override fun read(address: UShort): UByte = cart.read(address)

    override fun write(
        address: UShort,
        data: UByte,
    ) = cart.write(address, data)
}

class GBUS(
    private val chrrom: CHRROM,
) : AbstractDevice() {
    override val type = DeviceType.GBUS
    override val size = 0x3FFF
    override val base = 0x0000

    override fun read(address: UShort): UByte {
        if (address < 0x2000u) {
            return chrrom.read(address)
        }
        return 0xFFu
    }

    override fun write(
        address: UShort,
        data: UByte,
    ) {
        if (address < 0x2000u) {
            chrrom.write(address, data)
        }
    }
}
