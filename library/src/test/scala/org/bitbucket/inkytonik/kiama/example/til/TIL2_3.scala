/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2008-2017 Anthony M Sloane, Macquarie University.
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
package example.til

/**
 * Move all declarations to the start of the program.
 */
class TIL2_3 extends TransformingMain {

    import TILTree._
    import org.bitbucket.inkytonik.kiama.rewriting.Rewriter._

    val parsers = new TIL1_1Parsers(positions)
    val parser = parsers.program

    def transform(ast : Program) : Program = {
        val decls = Vector.newBuilder[Decl]
        val getandremovedecls =
            everywhere(rule[List[Stat]] {
                case (d : Decl) :: ss =>
                    decls += d
                    ss
            })
        val Program(stmts) = rewrite(getandremovedecls)(ast)
        Program(decls.result.toList ++ stmts)
    }

}

object TIL2_3Main extends TIL2_3
