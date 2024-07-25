package com.mooncell07.cecc.tests

import com.mooncell07.cecc.core.Bus
import com.mooncell07.cecc.core.CPU
import com.mooncell07.cecc.core.buildInstructionTable

open class BaseEmulator {
    val bus = Bus()
    val cpu = CPU(bus)

    init {
        buildInstructionTable()
    }
}
