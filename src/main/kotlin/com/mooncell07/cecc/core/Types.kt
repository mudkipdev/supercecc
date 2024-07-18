@file:Suppress("ktlint")

package com.mooncell07.cecc.core

enum class RegType { NONE, A, X, Y, SP, SR }

enum class FlagType { NONE, C, Z, I, D, V, N }

// Opcode Labels for generalised destination types differ from the standard labels.
// Register Transfer opcodes are exempted.
enum class InstructionType {
    BRK, OR, ASL, PUSH,
    BPL, CLEAR, JSR, AND,
    BIT, ROL, PULL, BRANCH,
    SET, RTI, EOR, LSR,
    JMP, RTS, ADC, ROR,
    STORE, DECREMENT, TXA, TYA,
    TXS, TAY, TAX, TSX,
    LOAD, COMPARE, INCREMENT, SBC,
    NOP,
}

enum class AddressingMode {
    ACCUMULATOR, ABSOLUTE, ABSOLUTE_X, ABSOLUTE_Y,
    IMMEDIATE, IMPLIED, INDIRECT, X_INDIRECT,
    INDIRECT_Y, RELATIVE, ZEROPAGE, ZEROPAGE_X,
    ZEROPAGE_Y,
}

typealias RT = RegType
typealias FT = FlagType
typealias IT = InstructionType
typealias AM = AddressingMode
