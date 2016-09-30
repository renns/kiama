/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2011-2016 Anthony M Sloane, Macquarie University.
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
package example.oberon0
package L3.source

import base.source.{
    Block,
    Declaration,
    Expression,
    IdnDef,
    IdnUse,
    SourceNode,
    Statement
}
import L0.source.TypeDef

/**
 * Procedure declarations.
 */
case class ProcDecl(idndef : IdnDef, params : Vector[FPSection], body : Block,
    idnuse : IdnUse) extends Declaration

/**
 * Non-terminal type for parameter passing modes.
 */
sealed abstract class Mode extends SourceNode

/**
 * Pass by variable (reference) mode.
 */
case class VarMode() extends Mode

/**
 * Pass by value mode.
 */
case class ValMode() extends Mode

/**
 * Formal parameter sections.
 */
case class FPSection(mode : Mode, idndefs : Vector[IdnDef], tipe : TypeDef) extends SourceNode

/**
 * Call statements.
 */
case class Call(idnuse : IdnUse, params : Vector[Expression]) extends Statement
