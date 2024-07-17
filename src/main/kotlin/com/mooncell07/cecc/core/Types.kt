package com.mooncell07.cecc.core

enum class RegType { NONE, A, X, Y, SP, SR }

enum class FlagType { NONE, C, Z, I, D, V, N }

// Q suffixed labels have generalised destination types.
enum class InstructionType {
    BRK,
    ORQ,
    ASL,
    PHQ,
    BPL,
    CLQ,
    JSR,
    AND,
    BIT,
    ROL,
    PLQ,
    BRQ,
}

enum class AddressingMode {
    ACCUMULATOR,
    ABSOLUTE,
    ABSOLUTE_X,
    ABSOLUTE_Y,
    IMMEDIATE,
    IMPLIED,
    INDIRECT,
    X_INDIRECT,
    INDIRECT_Y,
    RELATIVE,
    ZEROPAGE,
    ZEROPAGE_X,
    ZEROPAGE_Y,
}

typealias RT = RegType
typealias FT = FlagType
typealias IT = InstructionType
typealias AM = AddressingMode
