package com.mooncell07.cecc.tests

import com.mooncell07.cecc.core.Bus
import com.mooncell07.cecc.core.CPU
import com.mooncell07.cecc.core.buildInstructionTable

open class BaseEmulator {
    val cpu = CPU(bus = Bus())

    init {
        buildInstructionTable()
    }
}
