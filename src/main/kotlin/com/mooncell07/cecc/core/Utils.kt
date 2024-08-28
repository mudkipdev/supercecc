package com.mooncell07.cecc.core

fun mostSignificantByte(value: UShort): UByte = (value.toInt() shr 8).toUByte()

fun leastSignificantByte(value: UShort): UByte = (value.toInt() and 0xFF).toUByte()

fun testBit(value: Int, position: Int): Boolean = value and (1 shl position) != 0

fun concat(
    hi: UByte,
    lo: UByte,
): UShort = ((hi.toInt() shl 8) or lo.toInt()).toUShort()

fun setBit(value: Int, position: Int): Int = value or (1 shl position)

fun clearBit(value: Int, position: Int): Int = value and (1 shl position).inv()

fun handleBit(
    value: Int,
    position: Int,
    flagValue: Boolean,
) = if (flagValue) setBit(value, position) else clearBit(value, position)
