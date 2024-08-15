package com.mooncell07.cecc.core

class ZeroPage : Device() {
    override val type = DT.ZEROPAGE
    override val size = 0x00FF
    override val base = 0x0000
    override val area: UByteArray = UByteArray(size) { 0u }
}

class Stack : Device() {
    override val type = DT.STACK
    override val size = 0x00FF
    override val base = 0x0100
    override val area: UByteArray = UByteArray(size) { 0u }
}
