package com.mooncell07.cecc.core

import com.mooncell07.cecc.core.graphics.PPU

class Clock(
    private val ppu: PPU,
) {
    private var cycles: Int = 0

    fun tick() {
        cycles++
        ppu.tick()
    }
}
