package com.mooncell07.cecc.core.graphics

import com.mooncell07.cecc.core.FetcherState

class Fetcher {
    private var state: FetcherState = FetcherState.NT
    private var cycles = 2
    private var shiftRegister = MutableList(0xF) { 0 }

    fun tick() {
        if (cycles != 2) {
            cycles--
            return
        } else {
            cycles = 2
        }

        when (state) {
            FetcherState.NT -> {
                state = FetcherState.AT
            }
            FetcherState.AT -> {
                state = FetcherState.BGLSBITS
            }
            FetcherState.BGLSBITS -> {
                state = FetcherState.BGMSBITS
            }
            FetcherState.BGMSBITS -> {
                state = FetcherState.NT
            }
        }
    }
}
