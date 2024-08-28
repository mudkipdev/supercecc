package com.mooncell07.cecc.tests

import com.mooncell07.cecc.core.*
import com.mooncell07.cecc.core.graphics.PPU

class DebugDevice : AbstractDevice() {
    override val type = DeviceType.DEBUG
    override val size = 0xFFFF
    override val base = 0x0000

    private val area: UByteArray = UByteArray(0x10000) { 0u }
    private var noCycle: Boolean = false

    var cycles: MutableList<MutableList<Any>> = mutableListOf(mutableListOf(Any()))

    fun stopLogging() {
        noCycle = true
    }

    fun startLogging() {
        noCycle = false
    }

    override fun read(address: UShort): UByte {
        val result = area[address.toInt()]
        if (!noCycle) {
            cycles.add(mutableListOf(address.toDouble(), result.toDouble(), "read"))
        }
        return result
    }

    override fun write(
        address: UShort,
        data: UByte,
    ) {
        area[address.toInt()] = data
        if (!noCycle) {
            cycles.add(mutableListOf(address.toDouble(), data.toDouble(), "write"))
        }
    }
}

open class BaseEmulator {
    private val ppuRegs = PPURegisters()
    private val clock = Clock(PPU(ppuRegs))
    val debugDevice = DebugDevice()
    val bus = Bus(clock, debugDevice)
    val cpu = CPU(bus)

    init {
        buildInstructionTable()
        cpu.PC = 0xC000u
    }
}
