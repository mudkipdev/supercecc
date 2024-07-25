package com.mooncell07.cecc.core

fun MSB(value: UShort): UByte = (value.toInt() shr 8).toUByte()

fun LSB(value: UShort): UByte = (value.toInt() and 0xFF).toUByte()

fun testBit(
    value: Int,
    pos: Int,
): Int = (value and (1 shr pos))

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
