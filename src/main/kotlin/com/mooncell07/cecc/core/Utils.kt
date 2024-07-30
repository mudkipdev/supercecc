package com.mooncell07.cecc.core

fun MSB(value: UShort): UByte = (value.toInt() shr 8).toUByte()

fun LSB(value: UShort): UByte = (value.toInt() and 0xFF).toUByte()

fun testBit(
    value: Int,
    pos: Int,
): Boolean = value and (1 shl pos) != 0

fun concat(
    hi: UByte,
    lo: UByte,
): UShort = ((hi.toInt() shl 8) or lo.toInt()).toUShort()

fun setBit(
    v: Int,
    pos: Int,
): Int = v or (1 shl pos)

fun clearBit(
    v: Int,
    pos: Int,
): Int = v and (1 shl pos).inv()

fun handleBit(
    v: Int,
    pos: Int,
    flagv: Boolean,
) = if (flagv) setBit(v, pos) else clearBit(v, pos)
