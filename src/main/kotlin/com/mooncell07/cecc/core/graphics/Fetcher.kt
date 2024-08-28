package com.mooncell07.cecc.core.graphics

import com.mooncell07.cecc.core._FetcherState

class Fetcher {
    private var state: _FetcherState = _FetcherState.NT
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
            _FetcherState.NT -> {
                state = _FetcherState.AT
            }
            _FetcherState.AT -> {
                state = _FetcherState.BGLSBITS
            }
            _FetcherState.BGLSBITS -> {
                state = _FetcherState.BGMSBITS
            }
            _FetcherState.BGMSBITS -> {
                state = _FetcherState.NT
            }
        }
    }
}
