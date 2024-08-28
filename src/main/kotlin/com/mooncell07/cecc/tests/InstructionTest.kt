package com.mooncell07.cecc.tests

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.mooncell07.cecc.core.*
import java.io.File
import kotlin.system.exitProcess

data class State(
    @SerializedName("pc") var PC: UShort,
    @SerializedName("s") var SP: UByte,
    @SerializedName("a") var A: UByte,
    @SerializedName("x") var X: UByte,
    @SerializedName("y") var Y: UByte,
    @SerializedName("p") var SR: UByte,
    @SerializedName("ram") var ram: MutableList<List<Int>>,
) {
    override fun toString(): String =
        """State(PC=${PC.toHexString(HexFormat.UpperCase)}, 
            |SP=${SP.toHexString(HexFormat.UpperCase)}, 
            |A=${A.toHexString(HexFormat.UpperCase)}, 
            |X=${X.toHexString(HexFormat.UpperCase)}, 
            |Y=${Y.toHexString(HexFormat.UpperCase)}, 
            |SR=${SR.toHexString(HexFormat.UpperCase)}, 
            |ram=${ ram.map {innerList ->
            innerList.map { it.toUShort().toHexString(HexFormat.UpperCase) }
        }})
        """.trimMargin().replace("\n", "")
}

data class Test(
    @SerializedName("name") val name: String,
    @SerializedName("initial") val initial: State,
    @SerializedName("final") val final: State,
    @SerializedName("cycles") val cycles: List<List<Any>>,
)

class InstructionTest(
    file: File,
) : BaseEmulator() {
    private var tests: Array<Test>
    private val opcode: String
    private val after: State = State(0u, 0u, 0u, 0u, 0u, 0u, mutableListOf(listOf()))

    init {
        val gson = Gson()
        opcode = file.name.removeSuffix(".json").uppercase()
        tests = gson.fromJson(file.readText(), Array<Test>::class.java)
    }

    private fun setEmuState(test: Test) {
        cpu.PC = test.initial.PC
        cpu[RegisterType.A] = test.initial.A
        cpu[RegisterType.X] = test.initial.X
        cpu[RegisterType.Y] = test.initial.Y
        cpu[RegisterType.SP] = test.initial.SP
        cpu[RegisterType.SR] = test.initial.SR

        for (ramState in test.initial.ram) {
            bus.write(ramState[0].toUShort(), ramState[1].toUByte())
        }
        debugDevice.cycles.clear()
    }

    private fun parseState(test: Test) {
        after.PC = cpu.PC
        after.A = cpu[RegisterType.A]
        after.X = cpu[RegisterType.X]
        after.Y = cpu[RegisterType.Y]
        after.SP = cpu[RegisterType.SP]
        after.SR = cpu[RegisterType.SR]

        after.ram = MutableList(test.final.ram.size) { listOf(2) }
        for ((i, ramState) in test.final.ram.withIndex()) {
            after.ram[i] = listOf(ramState[0], bus.read(ramState[0].toUShort()).toInt())
        }
    }

    private fun prettyCycles(c: List<List<Any>>) =
        "${c.map {
            Triple(
                (it[0] as Number).toInt().toUShort().toHexString(HexFormat.UpperCase),
                (it[1] as Number).toInt().toUByte().toHexString(HexFormat.UpperCase),
                it[2],
            )
        }}"

    private fun compareStates(
        index: Int,
        test: Test,
    ) {
        val dataTest = test.final != after
        val cycleTest = (debugDevice.cycles != test.cycles)
        if (dataTest or cycleTest) {
            println(
                """[
                    |$$opcode FAILED @ 
                    |<TEST: $index 
                    |NAME: ${test.name.uppercase()} 
                    |MISMATCH TYPE: ${if (dataTest and cycleTest) {
                    "BOTH"
                } else if (dataTest) {
                    "DATA"
                } else {
                    "CYCLE"
                }}
                    |>]
                """.trimMargin().replace("\n", ""),
            )
            println(
                """
                |DATA LOG:
                |MINE: ${test.final}
                |YOURS: $after
                |
                |CYCLE LOG:
                |MINE: ${prettyCycles(test.cycles)}
                |YOURS: ${prettyCycles(debugDevice.cycles)}
                """.trimMargin(),
            )
            exitProcess(1)
        }
    }

    private fun compare(
        index: Int,
        test: Test,
    ) {
        parseState(test)
        compareStates(index, test)
    }

    fun run() {
        for ((i, test) in tests.withIndex()) {
            debugDevice.stopLogging()
            setEmuState(test)
            debugDevice.startLogging()
            cpu.tick()
            debugDevice.stopLogging()
            compare(i, test)
        }
        println("[$$opcode]: PASSED!")
    }
}

fun main(args: Array<String>) {
    if (args[0] == "--batch") {
        for (t in File(args[1]).listFiles()!!) {
            if (!t.isDirectory) {
                // When a directory of tests is passed, such as `adc-tests`, `and-tests` etc
                val iTest = InstructionTest(t)
                iTest.run()
            } else {
                // When a directory of subdirectories containing tests is passed, such as `json-tests`
                for (testFile in t.listFiles()!!) {
                    val iTest = InstructionTest(testFile)
                    iTest.run()
                }
            }
        }
    } else {
        // When the test file is directly passed
        val iTest = InstructionTest(File(args[0]))
        iTest.run()
    }
    println("\nALL TESTS PASSED!")
}
