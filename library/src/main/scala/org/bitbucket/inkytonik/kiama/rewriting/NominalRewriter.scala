/*
 * This file is part of Kiama.
 *
 * Copyright (C) 2013-2016 Anthony M Sloane, Macquarie University.
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
package rewriting

/**
 * Abstract syntax constructs that are common to all nominal rewriters.
 * These definitions need to be separate from the NominalRewriter class
 * so that the classes here don't get an outer field referring to an
 * instance of that class.
 */
object NominalTree {

    /**
     * A name comprising a base string with an optional integer index. The
     * index defaults to being omitted.
     */
    case class Name(base : String, opti : Option[Int] = None) {
        override def toString : String = base + opti.getOrElse("")
    }

    /**
     * A generic abstract binding of a name in a term.
     */
    case class Bind(name : Name, term : Any)

    /**
     * A transposition of two names is just a tuple.
     */
    type Trans = (Name, Name)

}

/**
 * An extension of strategy-based term rewriting with special support for
 * nominal rewriting along the lines of FreshML and the FreshLib library
 * for Haskell. See Scrap your Nameplate, James Cheney, ICFP 2005 for a
 * description of the ideas and the FreshLib library.
 */
class NominalRewriter extends Rewriter {

    import NominalTree._
    import org.bitbucket.inkytonik.kiama.util.Counter

    /**
     * Swap two names (given by `tr`) throughout a term `t`.
     */
    def swap[T](tr : Trans)(t : T) : T = {
        val s = everywhere(rule[Name] {
            case n => if (n == tr._1) tr._2
            else if (n == tr._2) tr._1
            else n
        })
        rewrite(s)(t)
    }

    /**
     * Is the name `a` fresh (not free) in a term?
     */
    def fresh(a : Name)(t : Any) : Boolean =
        t match {
            case n : Name    => a != n
            case Bind(b, t)  => (a == b) || fresh(a)(t)
            case p : Product => p.productIterator.forall(c => fresh(a)(c))
            case _           => true
        }

    /**
     * Alpha equivalence of two terms.
     */
    def alphaequiv(a1 : Any, a2 : Any) : Boolean =
        (a1, a2) match {
            case (n1 : Name, n2 : Name) =>
                n1 == n2
            case (Bind(a, x), Bind(b, y)) =>
                ((a == b) && alphaequiv(x, y)) ||
                    (fresh(a)(y) && alphaequiv(x, swap(a, b)(y)))
            case (p1 : Product, p2 : Product) =>
                (p1.productPrefix == p2.productPrefix) &&
                    p1.productIterator.zip(p2.productIterator).forall {
                        case (x, y) => alphaequiv(x, y)
                    }
            case _ =>
                a1 == a2
        }

    /**
     * An extractor pattern for terms that contain a single name child.
     */
    object HasVar {
        def unapply(t : Product) : Option[Name] =
            if (t.productArity == 1)
                t.productElement(0) match {
                    case n : Name => Some(n)
                    case _        => None
                }
            else
                None
    }

    /**
     * Counter to use to produce unique names.
     */
    val uniqueNameCounter = new Counter

    /**
     * Make a unique name using an old name as the base.
     */
    def genName(oldname : Name) : Name = {
        Name(oldname.base, Some(uniqueNameCounter.next()))
    }

    /**
     * Alternative extractor for Bind constructs. Decomposes an abstraction
     * returning the components after freshening the bound name.
     */
    object Binding {
        def unapply(b : Bind) : Option[(Name, Any)] = {
            val n = genName(b.name)
            Some((n, swap(n, b.name)(b.term)))
        }
    }

    /**
     * Substitution of `t1` for free occurrences of `n` in a term.
     */
    def subst[T](n : Name, t1 : Any) : T => T =
        rewrite(alltd(
            rule[Any] {
                case HasVar(m) if n == m =>
                    t1
                case Binding(a, x) =>
                    val y = subst(n, t1)(x)
                    Bind(a, y)
            }
        ))

    /**
     * Free variables in an term.
     */
    def fv(t : Any) : Set[Name] =
        t match {
            case n : Name   => Set(n)
            case Bind(b, t) => fv(t) - b
            case p : Product => p.productIterator.foldLeft(Set[Name]()) {
                case (s, c) => s | fv(c)
            }
            case _ => Set()
        }

}
