@file:OptIn(ExperimentalUnsignedTypes::class)
@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.mooncell07.cecc

import com.mooncell07.cecc.core.*

class Emulator {
    private val cart = Cartridge("roms/nestest.nes")

    @OptIn(ExperimentalUnsignedTypes::class)
    private val cpu = CPU(Register(), Bus(cart))

    init {
        buildInstructionTable()
    }

    fun tick() {
        for (i in 0..5) {
            cpu.tick()
        }
    }
}

fun main() {
    val emu = Emulator()
    emu.tick()
}
