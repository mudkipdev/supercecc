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

        state = when (state) {
            FetcherState.NT -> FetcherState.AT
            FetcherState.AT -> FetcherState.BGLSBITS
            FetcherState.BGLSBITS -> FetcherState.BGMSBITS
            FetcherState.BGMSBITS -> FetcherState.NT
        }
    }
}
