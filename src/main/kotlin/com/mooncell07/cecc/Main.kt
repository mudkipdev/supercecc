@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.mooncell07.cecc

import com.mooncell07.cecc.core.*

class Emulator {
    private val cart = Cartridge("roms/nestest.nes")
    private val cpu = CPU(Register(), Bus(cart))

    init {
        buildInstructionTable()
    }

    fun tick() {
        cpu.tick()
    }
}

fun main() {
    val emu = Emulator()
    emu.tick()
}
