package cecc.core

data class Registers(
    var A: UByte = 0u,
    var X: UByte = 0u,
    var Y: UByte = 0u,
    var PC: UShort = 0xFFFCu,
    var SP: UByte = 0xFDu,
    var SR: UByte = 0u
)

data class Flags(
    var C: Boolean = false,
    var Z: Boolean = false,
    var I: Boolean = true,
    var D: Boolean = false,
    var V: Boolean = false,
    var N: Boolean = false
)
