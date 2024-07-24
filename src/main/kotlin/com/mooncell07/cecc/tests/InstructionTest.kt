package com.mooncell07.cecc.tests

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mooncell07.cecc.core.*
import java.io.File

data class State(
    @SerializedName("pc") var PC: UShort,
    @SerializedName("s") var SP: UByte,
    @SerializedName("a") var A: UByte,
    @SerializedName("x") var X: UByte,
    @SerializedName("y") var Y: UByte,
    @SerializedName("p") var SR: UByte,
    @SerializedName("ram") var ram: MutableList<List<Int>>,
)

data class Test(
    @SerializedName("name") val name: String,
    @SerializedName("initial") val initial: State,
    @SerializedName("final") val final: State,
    @SerializedName("cycles") val cycles: List<List<Any>>,
)

class InstructionTest(
    filepath: String,
) : BaseEmulator() {
    private var tests: Array<Test>
    private val after: State = State(0u, 0u, 0u, 0u, 0u, 0u, mutableListOf(listOf()))

    init {
        val gson = Gson()
        val f = File(filepath).readText()
        tests = gson.fromJson(f, Array<Test>::class.java)
    }

    private fun setEmuState(test: Test) {
        cpu.reg.PC = test.initial.PC
        cpu.reg[RT.A] = test.initial.A
        cpu.reg[RT.X] = test.initial.X
        cpu.reg[RT.Y] = test.initial.Y
        cpu.reg[RT.SP] = test.initial.SP
        cpu.reg[RT.SR] = test.initial.SR

        for (ramState in test.initial.ram) {
            cpu.bus.writeByte(ramState[0].toUShort(), ramState[1].toUByte())
        }
    }

    private fun parseState(test: Test) {
        after.PC = cpu.reg.PC
        after.A = cpu.reg[RT.A]
        after.X = cpu.reg[RT.X]
        after.Y = cpu.reg[RT.Y]
        after.SP = cpu.reg[RT.SP]
        after.SR = cpu.reg[RT.SR]

        after.ram = MutableList(test.final.ram.size) { listOf(2) }
        for ((i, ramState) in test.final.ram.withIndex()) {
            after.ram[i] = listOf(ramState[0], cpu.bus.readByte(ramState[0].toUShort()).toInt())
        }
    }

    private fun compare(test: Test) {
        parseState(test)
        if (test.final != after) {
            println("MINE: ${test.final}\nYOURS: $after\n\n")
        } else {
            println("PASSED!")
        }
    }

    fun run() {
        for (test in tests) {
            setEmuState(test)
            cpu.tick()
            compare(test)
        }
    }
}

fun main(args: Array<String>) {
    val iTest = InstructionTest(args[0])
    iTest.run()
}
