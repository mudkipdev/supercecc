package com.mooncell07.cecc.core

enum class RegisterType {
    NONE,
    A,
    X,
    Y,
    SP,
    SR
}

enum class FlagType {
    NONE,
    C,
    Z,
    I,
    D,
    B,
    UNUSED2_IGN,
    V,
    N
}

enum class DeviceType {
    EMPTY,
    DEBUG,
    BUS,
    CARTRIDGE,
    ZEROPAGE,
    STACK,
    RAMEX,
    CHRROM,
    GBUS,
    PPUREGISTERS
}

enum class PPURegisterType {
    PPUCTRL,
    PPUMASK,
    PPUSTATUS,
    OAMADDR,
    OAMDATA,
    PPUSCROLL,
    PPUADDR,
    PPUDATA,
}

enum class FetcherState {
    NT, AT, BGLSBITS, BGMSBITS
}

// Opcode Labels for generalised destination types differ from the standard labels.
// Register Transfer opcodes are exempted.
enum class InstructionType {
    NONE,
    BRK,
    ORA,
    ASL,
    PUSH,
    CLEAR,
    JSR,
    EOR,
    AND,
    BIT,
    ROL,
    PULL,
    BRSET,
    BRCLR,
    SET,
    RTI,
    LSR,
    JMP,
    RTS,
    ADC,
    ROR,
    STORE,
    DECREMENT,
    TXA,
    TYA,
    TXS,
    TAY,
    TAX,
    TSX,
    LOAD,
    COMPARE,
    INCREMENT,
    SBC,
    NOP
}

enum class AddressingMode {
    NONE,
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

enum class PPUState {
    PRERENDER,
    RENDER,
    POSTRENDER,
    VBLANK
}