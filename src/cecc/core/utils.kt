package cecc.core

fun setBit(v: Int, pos: Int): Int {
    return v or (1 shl pos)
}

fun clearBit(v: Int, pos: Int): Int {
    return v and (1 shl pos).inv()
}
