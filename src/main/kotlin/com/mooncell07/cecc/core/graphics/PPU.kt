package com.mooncell07.cecc.core.graphics

import com.mooncell07.cecc.core.PPURegisters
import com.mooncell07.cecc.core._PPUState

class PPU(
    private val regs: PPURegisters,
) {
    private var dots = 0
    private var scanline = 0
    private var state: _PPUState = _PPUState.PRERENDER
    private val fetcher: Fetcher = Fetcher()

    fun tick() {
        dots++
        if (dots == 341) {
            scanline++
            dots = 0
        }

        when (state) {
            _PPUState.RENDER -> {
                if (scanline == 240) state = _PPUState.POSTRENDER
            }
            _PPUState.POSTRENDER -> {
                if (scanline == 241) state = _PPUState.VBLANK
            }
            _PPUState.VBLANK -> {
                if (scanline == 261) state = _PPUState.PRERENDER
            }
            _PPUState.PRERENDER -> {
                if (scanline == 262) {
                    state = _PPUState.RENDER
                    scanline = 0
                }
            }
        }
    }
}
