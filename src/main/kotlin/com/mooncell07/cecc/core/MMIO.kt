package com.mooncell07.cecc.core

class PPURegisters : Device() {
    override val type = DeviceType.PPUREGISTERS
    override val size = 0x0007
    override val base = 0x2000
    override val area: UByteArray = UByteArray(size + 1) { 0u }

    operator fun get(type: PPURegisterType): UByte = area[type.ordinal]

    operator fun set(type: PPURegisterType, data: UByte) {
        area[type.ordinal] = data
    }
}
