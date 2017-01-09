/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2011-2017 Anthony M Sloane, Macquarie University.
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

package org.bitbucket.inkytonik.kiama
package example.prolog

import PrologTree.Literal
import org.bitbucket.inkytonik.kiama.output.PrettyPrinter
import org.bitbucket.inkytonik.kiama.util.{Emitter, REPLConfig, ParsingREPLWithConfig}

/**
 * Configuration for the Prolog REPL.
 */
class PrologConfig(args : Seq[String]) extends REPLConfig(args) {

    import org.rogach.scallop.{ArgType, ValueConverter}
    import PrologTree.Program
    import scala.reflect.runtime.universe.TypeTag

    /**
     * Convertor for database option.
     */
    val databaseConverter =
        new ValueConverter[Program] {

            val argType = ArgType.SINGLE

            def parse(s : List[(String, List[String])]) : Either[String, Option[Program]] =
                s match {
                    case List((_, List(filename))) =>
                        Main.makeDatabase(filename)
                    case _ =>
                        Right(None)
                }

            val tag = implicitly[TypeTag[Program]]

        }

    /**
     * The program that represents the facts and clauses that will be made available
     * to the queries that are entered in the REPL.
     */
    lazy val database = opt[Program]("database", descr = "Database of facts and clauses to use in queries",
        required = true)(databaseConverter)

}

/**
 * Conduct semantic analysis on the Prolog program in the file given as
 * the first command-line argument.  If the program is correct, enter an
 * interactive read-eval-print loop (REPL) to read queries.  For each
 * query, call the interpreter to evaluate it.
 */
object Main extends ParsingREPLWithConfig[Literal, PrologConfig] with PrettyPrinter {

    import org.bitbucket.inkytonik.kiama.parsing.Success
    import org.bitbucket.inkytonik.kiama.util.{
        Emitter,
        FileSource,
        OutputEmitter,
        Source,
        StringEmitter
    }
    import PrologTree.{Program, PrologTree}

    val banner = "Prolog interpreter (exit with end of file: ^Z on Windows, ^D on Mac, Linux, Unix"

    def createConfig(args : Seq[String]) : PrologConfig =
        new PrologConfig(args)

    val parsers = new SyntaxAnalyser(positions)
    val parser = parsers.query

    /**
     * Helper function to create the database from the given filename or return
     * a command-line error.
     */
    def makeDatabase(filename : String) : Either[String, Option[Program]] =
        try {
            // Parse the file
            val source = FileSource(filename)
            parsers.parseAll(parsers.program, source) match {
                // If parse worked, we get a source tree, check it
                case Success(dbtree, _) =>
                    // Pretty print the source tree
                    // config.error.emitln (pretty (any (dbtree)))
                    val tree = new PrologTree(dbtree)
                    val analyser = new SemanticAnalyser(tree)
                    val messages = analyser.errors
                    if (messages.length > 0) {
                        val emitter = new StringEmitter
                        report(messages, emitter)
                        Left(s"database file errors: ${emitter.result}")
                    } else {
                        Right(Some(dbtree))
                    }
                // If parse failed, we get an error message
                case f =>
                    Left(s"error parsing $filename: $f")
            }
        } catch {
            case e : java.io.FileNotFoundException =>
                Left(e.getMessage)
        }

    /**
     * The prompt to print before each line of input is read.
     */
    override val prompt = "?- "

    /**
     * The interpreter to use to evaluate queries.
     */
    val interpreter = new Interpreter

    /**
     * Process a query by passing it and the program to the interpreter.
     */
    def process(source : Source, querytree : Literal, config : PrologConfig) {
        interpreter.interpret(querytree, config.database(), config.output())
    }

}
