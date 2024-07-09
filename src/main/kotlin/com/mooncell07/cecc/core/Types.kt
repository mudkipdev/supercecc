package com.mooncell07.cecc.core

enum class RegType { A, X, Y, SP, SR }
enum class FlagType { C, Z, I, D, V, N }
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
    ZEROPAGE_Y
}