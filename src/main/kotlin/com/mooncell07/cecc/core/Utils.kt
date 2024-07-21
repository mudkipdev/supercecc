package com.mooncell07.cecc.core

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
