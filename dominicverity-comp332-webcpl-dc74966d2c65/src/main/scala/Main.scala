/*
 * This file is part of COMP332 Assignment 3 2019.
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
 * Main compiler driver.
 */

package webcpl

import org.bitbucket.inkytonik.kiama._
import util.Messaging
import util.PositionStore

/**
  * Parse the BCPL program in the file given as the
  * first command-line argument and print the source tree.
  */
object Main extends PositionStore with Messaging {

  import java.io.FileNotFoundException
  import output.PrettyPrinter.{any, layout}
  import parsing.{NoSuccess, Success}
  import util.FileSource
  import Messaging.message

  def main(args: Array[String]): Unit = {

    args.size match {

      // If there is exactly one command-line argument
      case 1 =>
        try {
          // Create a source for the argument file name
          val source = new FileSource(args(0))

          // Create a syntax analysis module
          val parsers = new SyntaxAnalysis(positions)

          // Parse the file
          parsers.parse(parsers.parser, source) match {
            // If it worked, we get a source tree
            case Success(sourcetree, _) =>
              // Pretty print the source tree
              println("\nProgram tree:\n-------------\n")
              println(layout(any(sourcetree), 100))

            // Parsing failed, so report it
            case res: NoSuccess =>
              val pos = res.next.position
              positions.setStart(res, pos)
              positions.setFinish(res, pos)
              val messages = message(res, res.message)
              println(formatMessages(messages))
          }
        } catch {
          case e: FileNotFoundException =>
            println(e.getMessage)
        }

      // Complain otherwise
      case _ =>
        println("usage: run file.b")
    }
  }
}
