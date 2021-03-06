package sml

import sml.instructions.NoOpInstruction
import java.io.File
import java.io.IOException
import java.util.Scanner
import kotlin.collections.ArrayList
import kotlin.reflect.full.primaryConstructor

/*
 * The machine language interpreter
 */
data class Machine(var pc: Int, val noOfRegisters: Int) {
    // The labels in the SML program, in the order in which
    // they appear (are defined) in the program

    val labels: Labels

    // The SML program, consisting of prog.size() instructions, each
    // of class sml.Instruction (or one of its subclasses)
    val prog: MutableList<Instruction>

    // The registers of the SML machine
    val registers: Registers

    // The program counter; it contains the index (in prog) of
    // the next instruction to be executed.

    init {
        labels = Labels()
        prog = ArrayList<Instruction>()
        registers = Registers(noOfRegisters)
    }

    /**
     * Print the program
     */
    override fun toString(): String {
        val s = StringBuffer()
        for (i in 0 until prog.size)
            s.append(prog[i].toString() + "\n")
        return s.toString()
    }

    /**
     * Execute the program in prog, beginning at instruction 0.
     * Precondition: the program and its labels have been stored properly.
     */
    fun execute() {
        while (pc < prog.size) {
            val ins = prog[pc++]
            ins.execute(this)
        }
    }

    // root of files
    private val PATH = System.getProperty("user.dir") + "/"
    // input line of file
    private var line: String = ""

    /**
     * translate the small program in the file into lab (the labels) and prog (the program)
     * return "no errors were detected"
     */
    fun readAndTranslate(file: String): Boolean {
        val fileName = PATH + file // source file of SML code
        return try {
            Scanner(File(fileName)).use { sc ->

                labels.reset()
                prog.clear()

                while (sc.hasNext()) {
                    line = sc.nextLine()
                    val label = scan()

                    if (label.length > 0) {
                        labels.addLabel(label)
                        prog.add(getInstruction(label))
                    }
                }
            }
            true
        } catch (ioE: IOException) {
            println("File: IO error " + ioE.message)
            false
        }
    }

    /**
     * line should consist of an MML instruction, with its label already removed.
     * Translate line into an instruction with label label and return the instruction
     */
    fun getInstruction(label: String): Instruction {

        val ins = scan()
        val defaultInstruction = NoOpInstruction(label, line)

        // form class name on the fly and obtain KClass reference
        var classRef = try {
            val className = ins.substring(0, 1).toUpperCase() + ins.substring(1) + "Instruction"
            Class.forName("sml.instructions.$className").kotlin
        } catch (exception: ClassNotFoundException) {
            Class.forName("sml.instructions.NoOpInstruction").kotlin
        }


        // obtain reference to primary constructor
        val primaryConstructor = classRef.primaryConstructor

        // create instruction as needed
        // if the instruction cannot be created return a NoOp
        return when (ins) {
            "bnz" -> primaryConstructor?.call(label, scanInt(), scan()) as Instruction
            "lin" -> primaryConstructor?.call(label, scanInt(), scanInt()) as Instruction
            "add", "sub", "mul", "div" -> primaryConstructor?.call(label, scanInt(), scanInt(), scanInt()) as Instruction
            "out" -> primaryConstructor?.call(label, scanInt()) as Instruction
            else -> defaultInstruction
        }
    }

    /*
     * Return the first word of line and remove it from line. If there is no
     * word, return ""
     */
    private fun scan(): String {
        line = line.trim { it <= ' ' }
        if (line.length == 0)
            return ""

        var i = 0
        while (i < line.length && line[i] != ' ' && line[i] != '\t') {
            i = i + 1
        }
        val word = line.substring(0, i)
        line = line.substring(i)
        return word
    }

    /**
     * Return the first word of line as an integer. If there is
     * any error, return the maximum int
     */
    private fun scanInt(): Int {
        val word = scan()
        if (word.length == 0) {
            return Integer.MAX_VALUE
        }

        return try {
            Integer.parseInt(word)
        } catch (e: NumberFormatException) {
            Integer.MAX_VALUE
        }
    }

    /**
     * Return the index of the instruction in prog
     * which corresponds to the passed label
     * Return -1 if instruction cannot be found
     */
    private fun getInstructionAddress(label: String): Int {
        for ((address, instr) in prog.withIndex()) {
            if (label == instr.toString().split(":")[0])
                return address
        }
        return -1
    }

    /**
     * Changes the value of the machine's program counter to point
     * to the instruction described by the passed label
     * If the passed label is not found, no change will happen
     */
    fun jumpToInstruction(label: String) {
        val position = getInstructionAddress(label)
        if (position != -1)
            pc = position
    }
}
