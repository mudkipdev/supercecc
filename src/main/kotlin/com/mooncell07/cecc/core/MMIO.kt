package com.mooncell07.cecc.core

class PPURegisters : Device() {
    override val type = DeviceType.PPUREGISTERS
    override val size = 0x0007
    override val base = 0x2000
    override val area: UByteArray = UByteArray(size + 1) { 0u }

    operator fun get(ppuRegType: PPURegisterType): UByte = area[ppuRegType.ordinal]

    operator fun set(
        ppuRegType: PPURegisterType,
        data: UByte,
    ) {
        area[ppuRegType.ordinal] = data
    }
}
