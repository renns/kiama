/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2008-2010 Anthony M Sloane, Macquarie University.
 *
 * Kiama is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Kiama is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Kiama.  (See files COPYING and COPYING.LESSER.)  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.kiama.example.oberon0.machine

import RISCISA._
import org.kiama.machine.Machine
import org.kiama.util.Console
import org.kiama.util.Emitter

/**
 * Abstract state machine simulation of a simple RISC architecture.  Run the
 * given code, reading input from console and emitting output using emitter.
 */
class RISC (code : Code, console : Console, emitter : Emitter)
        extends Machine ("RISC", emitter) {
    
    import org.kiama.util.Console

    /**
     * Debug flag. Set this to true in sub-classes or objects to obtain
     * tracing information during execution of the machine.
     */
    override def debug = false

    /**
     * Words are 32-bits long.
     */
    type Word = Int

    /**
     * A named register.
     */
    case class Reg (name : String) extends State[Int] (name)

    /**
     * Integer register file addressed by 0-31.
     */
    lazy val R = Array.tabulate (32) (i => Reg ("R" + i.toString))

    /**
     * Names for special registers.
     */
    lazy val PC = R (28)
    lazy val FP = R (29)
    lazy val SP = R (30)
    lazy val LNK = R (31)

    /**
     * Byte addressed store of words.
     */
    val Mem = new ParamState[Int,Int] ("Mem")

    /**
     * Condition code: zero.
     */
    val Z = new State[Boolean] ("Z")

    /**
     * Condition code: less than.
     */
    val N = new State[Boolean] ("N")

    /**
     * Halt flag.  Undefined until the machine is to stop executing.
     */
    val halt = new State[String] ("halt")

    /**
     * Initialise the machine.
     */
    override def init {
        PC := 0
        R (0) := 0
        Z := false
        N := false
        performUpdates
    }

    /**
     * The main rule of this machine.
     */
    def main {
        if (halt isUndefined)
            execute (code (PC))
    }

    /**
     * Execute a single instruction.
     */
    def execute (instr : Instr) {
        if (debug)
            emitter.emitln (name + " exec: " + instr)
        arithmetic (instr)
        memory (instr)
        control (instr)
        inputoutput (instr)
    }

    /**
     * Execute arithmetic instructions.
     */
    def arithmetic (instr : Instr) {
        instr match {
            case MOV (a, b, c)   => R (a) := R (c) << b
            case MOVI (a, b, im) => R (a) := im << b
            case MVN (a, b, c)   => R (a) := - (R (c) << b)
            case MVNI (a, b, im) => R (a) := - (im << b)
            case ADD (a, b, c)   => R (a) := R (b) + R (c)
            case ADDI (a, b, im) => R (a) := R (b) + im
            case SUB (a, b, c)   => R (a) := R (b) - R (c)
            case SUBI (a, b, im) => R (a) := R (b) - im
            case MUL (a, b, c)   => R (a) := R (b) * R (c)
            case MULI (a, b, im) => R (a) := R (b) * im
            case DIV (a, b, c)   => R (a) := R (b) / R (c)
            case DIVI (a, b, im) => R (a) := R (b) / im
            case MOD (a, b, c)   => R (a) := R (b) % R (c)
            case MODI (a, b, im) => R (a) := R (b) % im
            case CMP (b, c)      => Z := R (b) =:= R (c)
                                    N := R (b) < R (c)
            case CMPI (b, im)    => Z := R (b) =:= im
                                    N := R (b) < im
            case CHKI (a, im)    => if ((R (a) < 0) || (R (a) >= im))
                                        R (a) := 0
            case _ => ()
        }
    }

    /**
     * Execute memory instructions.
     */
    def memory (instr : Instr) {
        try {
            instr match {
                case LDW (a, b, im) => R (a) := Mem ((R (b) + im) / 4)
                case LDB (a, b, im) => halt := "LDB not implemented"
                case POP (a, b, im) => R (a) := Mem ((R (b) - im) / 4)
                                       R (b) := R (b) - im
                case STW (a, b, im) => Mem ((R (b) + im) / 4) := R (a)
                case STB (a, b, im) => halt := "STB not implemented"
                case PSH (a, b, im) => Mem (R (b) / 4) := R (a)
                                       R (b) := R (b) + im
                case _ => ()
            }
        }
        catch {
            case e =>
                println ("Exception at " + instr)
                e.printStackTrace
                println (Mem)
                halt := "Halt"
        }
    }

    /**
     * Execute control instructions, including default control step.
     */
    def control (instr : Instr) {
        instr match {
            case b : BEQ if (Z)        => PC := PC + b.disp
            case b : BNE if (!Z)       => PC := PC + b.disp
            case b : BLT if (N)        => PC := PC + b.disp
            case b : BGE if (!N)       => PC := PC + b.disp
            case b : BLE if (Z || N)   => PC := PC + b.disp
            case b : BGT if (!Z && !N) => PC := PC + b.disp
            case b : BR                => PC := PC + b.disp
            case b : BSR               => R (31) := PC + 1
                                          PC := PC + b.disp
            case RET (c) => PC := R (c)
                            if (R (c) =:= 0) halt := "Halt"
            case _  => PC := PC + 1
        }
    }

    /**
     * Execute input/output instructions.
     */
    def inputoutput (instr : Instr) {
        instr match {
            case RD (a)  => R (a) := console.readInt ("Enter integer: ")
            case WRD (c) => emitter.emit (R (c))
            case WRH (c) => emitter.emit ((R (c) : Int) toHexString)
            case WRL     => emitter.emitln
            case _       =>
        }
    }

}