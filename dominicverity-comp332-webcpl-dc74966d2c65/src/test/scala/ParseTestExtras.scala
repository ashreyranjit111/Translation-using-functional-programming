/*
 * This file is part of COMP332 Assignment 2/3 2019.
 *
 * weBCPL, a retro BCPL to WebAssembly compiler.
 *
 * © 2019, Dominic Verity and Anthony Sloane, Macquarie University.
 *         All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Tests of the parser of the BCPL language.
 */

package webcpl

import org.bitbucket.inkytonik.kiama.parsing.{ParseResult, NoSuccess, Success}
import org.scalatest.matchers.{Matcher, MatchResult}

trait ParseTestExtras {
  /**
  * Parse failure test that doesn't require the specification of a
  * failure location or message.
  */ 
  def failParse[T]() =
  new Matcher[ParseResult[T]] {
    def apply(result : ParseResult[T]) =
      result match {
        case Success(value, in) =>
          MatchResult(false, s"""Parse succeeded with "$value" at ${in.format}""", "NOT USED")
        case NoSuccess(_, in) =>
          MatchResult(
            true, s"Parse failed at ${in.format}!", "NOT USED")
      }
  }

  /**
    * Parse failure test that only requires the specification of a 
    * failure location.
    */
    def failParseAt[T](line: Int, column: Int) =
    new Matcher[ParseResult[T]] {
      def apply(result : ParseResult[T]) =
        result match {
          case Success(value, in) =>
            MatchResult(false, s"""Parse succeeded with "$value" at ${in.format}""", "NOT USED")
          case res @ NoSuccess(_, in) =>
            MatchResult(
              (in.position.line == line) && (in.position.column == column),
              {
                  val buf = new scala.collection.mutable.ListBuffer[String]()
                  if (in.position.line != line)
                      buf += s"line is ${in.position.line} not $line"
                  if (in.position.column != column)
                      buf += s"column is ${in.position.column} not $column"
                  buf.mkString(s"Wrong parse ${res.kind}: ", ", ", "")
              },
              {
                  val buf = new scala.collection.mutable.ListBuffer[String]()
                  if (in.position.line == line)
                      buf += s"line is $line"
                  if (in.position.column == column)
                      buf += s"column is $column"
                  buf.mkString(s"Expected parse ${res.kind}: ", ", ", "")
              })
        }
    }
}