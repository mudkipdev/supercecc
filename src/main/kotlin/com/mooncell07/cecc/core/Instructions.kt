package com.mooncell07.cecc.core

data class INSTR(
    val insType: IT,
    val addrMode: AM,
    val regType: RT,
    val flagType: FT,
)

val INSTAB: Array<INSTR?> = arrayOfNulls(256)

fun buildInstructionTable() {
    INSTAB[0x00] = INSTR(IT.BRK, AM.IMPLIED, RT.NONE, FT.NONE)
    INSTAB[0x01] = INSTR(IT.ORQ, AM.X_INDIRECT, RT.A, FT.NONE)
    INSTAB[0x05] = INSTR(IT.ORQ, AM.ZEROPAGE, RT.A, FT.NONE)
    INSTAB[0x06] = INSTR(IT.ASL, AM.ZEROPAGE, RT.NONE, FT.C)
    INSTAB[0x08] = INSTR(IT.PHQ, AM.IMPLIED, RT.SR, FT.NONE)
    INSTAB[0x09] = INSTR(IT.ORQ, AM.IMMEDIATE, RT.A, FT.NONE)
    INSTAB[0x0A] = INSTR(IT.ASL, AM.ZEROPAGE, RT.NONE, FT.C)
    INSTAB[0x0D] = INSTR(IT.ORQ, AM.ABSOLUTE, RT.A, FT.NONE)
    INSTAB[0x0E] = INSTR(IT.ASL, AM.ABSOLUTE, RT.NONE, FT.C)
}
