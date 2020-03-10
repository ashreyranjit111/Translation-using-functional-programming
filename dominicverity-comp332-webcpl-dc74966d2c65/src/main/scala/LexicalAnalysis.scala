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
 * Lexical parsers for the BCPL language.
 */

package webcpl

import org.bitbucket.inkytonik.kiama.parsing.{
  Parsers,
  ParsersBase,
  Input,
  Failure,
  Success,
  ParseResult
}

/**
  * Module containing parsers for the lexical structure of BCPL.
  */
trait LexicalAnalysis extends Parsers {
  self: ParsersBase =>

  import java.lang.Long.parseLong
  import scala.util.matching.Regex

  /*
   * Switch off Tony's automatic whitespace stripping and explicitly
   * strip whitespace at the start of each lexeme parser.
   */
  parsingWhitespace = true

  /**
    * Trim whitespace from input stream, swallows any errors unless they
    * occur at the end of input.
    */
  def trimWhitespace(w: Parser[String]): Parser[String] =
    new Parser[String] {
      def apply(in: Input): ParseResult[String] =
        w(in) match {
          case failure @ Failure(_, next) =>
            if (next.atEnd)
              failure
            else
              Success("", in)
          case success =>
            success
        }
    }

  /**
    * Lexeme parser, strips specified whitespace before parsing a lexeme.
    */
  def lexeme[T](w: Parser[String], p: Parser[T]): Parser[T] =
    trimWhitespace(w) ~> p

  /**
    * Lexeme parser, strips standard whitespace before parsing a lexeme.
    *
    * We use the character class `\R` here to match line endings.
    * This ensures that we correctly handle all of the end-line variants
    * in un*x, MacOSX, MS Windows, and unicode.
    */
  def lexeme[T](p: Parser[T]): Parser[T] =
    lexeme("""([\h\v]|(//.*?(\R|\z)))*""".r, p)

  /**
    * Phrase parser, overridden to add whitespace parsing to the end of
    * the wrapped parser.
    */
  override def phrase[T](p: => Parser[T]): Parser[T] =
    super.phrase(p <~ """([\h\v]|(//.*?(\R|\z)))*""".r)

  implicit class ExtraParserOps[T](p: Parser[T]) {
    def !(m: String) =
      new Parser[T] {
        def apply(in: Input): ParseResult[T] =
          p(in) match {
            case Failure(_, next) => Failure(m, next)
            case success          => success
          }
      }
  }

  /*
   * It takes a little bit of effort to get the parsing of string
   * elementExp to conform to the BCPL standard. Only stray into the
   * following code if you are curious about how this kind of lexical
   * fiddle-faddle is achieved in this kind of parser.
   */

  private val escapeHexRegex: Regex = """[0-7][0-9a-fA-F]""".r
  private val escapeOctalRegex: Regex = """[0-1][0-7]{2}""".r
  private val escapeUnicodeRegex: Regex = """[0-3][0-9a-fA-F]{5}""".r
  private val escapeWhitespaceRegex: Regex = """([\h\v]|//.*?\R)+\*""".r

  def codepointToUTF8(c: Long): Vector[Int] =
    if (c >= 0 && c <= 0x7F)
      Vector(c.toInt)
    else if (c >= 0x80 && c <= 0x7FF)
      Vector((0xC0 | ((c >> 6) & 0x1F)).toInt, (0x80 | (c & 0x3F)).toInt)
    else if (c >= 0x800 && c <= 0xFFFF)
      Vector(
        (0xE0 | ((c >> 12) & 0x0F)).toInt,
        (0x80 | ((c >> 6) & 0x3F)).toInt,
        (0x80 | (c & 0x3F)).toInt
      )
    else if (c >= 0x10000 && c <= 0x10FFFF)
      Vector(
        (0xF0 | ((c >> 18) & 0x07)).toInt,
        (0x80 | ((c >> 12) & 0x3F)).toInt,
        (0x80 | ((c >> 6) & 0x3F)).toInt,
        (0x80 | (c & 0x3F)).toInt
      )
    else Vector()

  def encodeUTF8(c: Long): PackratParser[Vector[Int]] =
    codepointToUTF8(c) match {
      case Vector() => failure("illegal unicode codepoint")
      case v        => success(v)
    }

  lazy val parseCharEscape: PackratParser[Vector[Int]] =
    elem('*') ~> (
      elem('n') ^^^ Vector(0x0A) |
      elem('c') ^^^ Vector(0x0D) |
      elem('p') ^^^ Vector(0x0F) |
      elem('s') ^^^ Vector(0x20) |
      elem('b') ^^^ Vector(0x08) |
      elem('t') ^^^ Vector(0x09) |
      elem('e') ^^^ Vector(0x1B) |
      elem('*') ^^^ Vector(0x22) |
      elem('\'') ^^^ Vector(0x27) |
      elem('"') ^^^ Vector(0x22) |
      elem('x') ~> 
        ((regex(escapeHexRegex) ^^ (s => codepointToUTF8(parseLong(s, 16)))) |
          error("illegal hex ASCII code")) |
      elem('u') ~> 
        ((regex(escapeUnicodeRegex) ^^ (s => codepointToUTF8(parseLong(s, 16)))) |
          error("illegal unicode codepoint")) |
      guard(regex("[0-9]".r)) ~> 
        ((regex(escapeOctalRegex) ^^ (s => codepointToUTF8(parseLong(s, 8)))) |
          error("illegal octal ASCII code")) |
      regex(escapeWhitespaceRegex) ^^^ Vector() |
      error("malformed character escape"))

  lazy val parseChar: PackratParser[Vector[Int]] =
    nocut(
      parseCharEscape |
        (regex("""[^\v"]""".r) ^^ (s => codepointToUTF8(s.codePointAt(0)))) |
        failure("illegal character")) 

  lazy val stringConst: PackratParser[Vector[Int]] =
    lexeme("\"" ~> (rep(parseChar) ^^ (_.flatten)) <~ 
      (literal("\"") ! "unterminated string literal"))

  lazy val charConst: PackratParser[Vector[Int]] =
    lexeme("'" ~> 
      (not(regex("""\*([\v\h]|//)|'""".r)) | failure("empty character literal")) ~>
      nocut(parseCharEscape | 
              regex("""[^\v]""".r) ^^ (s => codepointToUTF8(s.codePointAt(0))) |
              failure("end of line/input in character literal")) <~
      (literal("'") ! "unterminated character literal"))

  private def convertInteger(s: String, r: Int): Int =
    parseLong(
      s.filter(
        c =>
          ('0' <= c && c <= '9') ||
            ('a' <= c && c <= 'f') ||
            ('A' <= c && c <= 'F')
      ),
      r
    ).toInt

  lazy val integerConst: PackratParser[Int] =
    lexeme(
      regex("[0-9_]+".r) ^^ (convertInteger(_, 10)) |
        regex("#[bB][01_]+".r) ^^ (convertInteger(_, 2)) |
        regex("#[oO]?[0-7_]+".r) ^^ (convertInteger(_, 8)) |
        regex("#[xX][0-9a-fA-F_]+".r) ^^ (convertInteger(_, 16))
    ) ! "expecting integer literal"

  /**
    * Parses a legal BCPL identifier. Checks to ensure that the word parsed
    * is not a BCPL keyword.
    */
  lazy val identifier: PackratParser[String] =
    lexeme(
      (not(keywordBCPL) ! "expecting identifier but keyword found") ~>
        (regex("[a-zA-Z][a-zA-Z0-9_]*".r) ! "expecting identifier")
    )

  /*
   * BCPL operator symbols:
   *
   * `@`, `!`, `%`, `*`, `/`, `+`, `-`, `~`, `&`, `|`,
   * `=`, `~=`, `<=`, `>=`, `<`, `>`, `:`, `:=`, `::`,
   * `<<`, `>>`, `(`, `)`, `{`, `}`, `,`, `;`, `->`
   */
  lazy val at: PackratParser[String] = lexeme(literal("@")) ! "expecting '@'"
  lazy val unaryPling
      : PackratParser[String] = lexeme(literal("!")) ! "expecting unary '!'"
  lazy val unaryPercent
      : PackratParser[String] = lexeme(literal("%")) ! "expecting unary '%'"
  lazy val star: PackratParser[String] = lexeme(literal("*")) ! "expecting '*'"
  lazy val slash: PackratParser[String] = lexeme(literal("/")) ! "expecting '/'"
  lazy val unaryPlus
      : PackratParser[String] = lexeme(literal("+")) ! "expecting unary '+'"
  lazy val unaryMinus
      : PackratParser[String] = lexeme(regex("""-(?![\>])""".r)) ! "expecting unary '-'"
  lazy val apersand
      : PackratParser[String] = lexeme(literal("&")) ! "expecting '&'"
  lazy val pipe: PackratParser[String] = lexeme(literal("|")) ! "expecting '|'"
  lazy val equal: PackratParser[String] = lexeme(literal("=")) ! "expecting '='"
  lazy val notEqual
      : PackratParser[String] = lexeme(literal("~=")) ! "expecting '~='"
  lazy val less: PackratParser[String] = lexeme(regex("""\<(?![\=\<])""".r)) ! "expecting '<'"
  lazy val lessOrEqual
      : PackratParser[String] = lexeme(literal("<=")) ! "expecting '<='"
  lazy val greater
      : PackratParser[String] = lexeme(regex("""\>(?![\>\=])""".r)) ! "expecting '>'"
  lazy val greaterOrEqual
      : PackratParser[String] = lexeme(literal(">=")) ! "expecting '>='"
  lazy val colon
      : PackratParser[String] = lexeme(regex(""":(?![:\=])""".r)) ! "expecting ':'"
  lazy val assign
      : PackratParser[String] = lexeme(literal(":=")) ! "expecting ':='"
  lazy val doubleColon
      : PackratParser[String] = lexeme(literal("::")) ! "expecting '::'"
  lazy val shiftLeft
      : PackratParser[String] = lexeme(literal("<<")) ! "expecting '<<'"
  lazy val shiftRight
      : PackratParser[String] = lexeme(literal(">>")) ! "expecting '>>'"
  lazy val leftParen
      : PackratParser[String] = lexeme(literal("(")) ! "expecting '('"
  lazy val rightParen
      : PackratParser[String] = lexeme(regex("""\)(?![\$])""".r)) ! "expecting ')'"
  lazy val leftBrace
      : PackratParser[String] = lexeme(regex("""\{|\$\(""".r)) ! "expecting '{' or '$('"
  lazy val rightBrace
      : PackratParser[String] = lexeme(regex("""\}|\$\)""".r)) ! "expecting '}' or '$)'"
  lazy val comma: PackratParser[String] = lexeme(literal(",")) ! "expecting ','"
  lazy val rightArrow
      : PackratParser[String] = lexeme(literal("->")) ! "expecting '->'"
  lazy val question
      : PackratParser[String] = lexeme(literal("?")) ! "expecting '?'"

  /*
   * We handle binary `+`, `-`, `!`,`%` and the separator `;` as special cases to
   * implement the folling related rules:
   *
   * - Semi-colons may be eliminated at the end of a line
   * - Operator symbols which can be used in unary and binary forms must be
   *   interpreted as unary if they are the first non-whitespace symbol on a line.
   */
  lazy val pling: PackratParser[String] =
    lexeme(regex("""\h*""".r), literal("!")) ! "expecting binary '!'"
  lazy val percent: PackratParser[String] =
    lexeme(regex("""\h*""".r), literal("%")) ! "expecting binary '%'"
  lazy val plus: PackratParser[String] =
    lexeme(regex("""\h*""".r), literal("+")) ! "expecting binary '+'"
  lazy val minus: PackratParser[String] =
    lexeme(regex("""\h*""".r), regex("""-(?![\>])""".r)) ! "expecting binary '-'"
  lazy val semiColon: PackratParser[String] =
    (regex("""\h*(;|(//.*?)?\R)""".r) ^^^ ";") ! "expecting statement separator"

  /**
    * Parses any legal BCPL keyword. This parser ensures that the keyword found
    * is not a prefix of an identifier.
    */
  lazy val keywordBCPL: PackratParser[String] =
    keywords(
      """[^a-zA-Z0-9_]""".r,
      List(
        "TRUE",
        "FALSE",
        "SLCT",
        "TABLE",
        "ABS",
        "NOT",
        "OF",
        "MOD",
        "REM",
        "EQV",
        "NEQV",
        "XOR",
        "VALOF",
        "RESULTIS",
        "GLOBAL",
        "STATIC",
        "MANIFEST",
        "IF",
        "UNLESS",
        "TEST",
        "DO",
        "THEN",
        "BE",
        "ELSE",
        "WHILE",
        "UNTIL",
        "REPEAT",
        "REPEATWHILE",
        "REPEATUNTIL",
        "FOR",
        "TO",
        "BY",
        "SWITCHON",
        "INTO",
        "CASE",
        "DEFAULT",
        "RETURN",
        "ENDCASE",
        "LOOP",
        "BREAK",
        "GOTO",
        "FINISH",
        "LET",
        "AND",
        "VEC"
      )
    ) |
      failure("keyword expected")

  /**
    * Parses a string as a keyword. Fails if the given keyword isn't found
    * or it isn't appropriately terminated.
    */
  def keyword(s: String) =
    lexeme(regex("""%s(?=[^A-Za-z0-9_]|\z)""".format(s).r)) ! s"expecting keyword '$s'"

  /*
   * BCPL keywords words:
   */
  lazy val TRUE: PackratParser[String] = keyword("TRUE")
  lazy val FALSE: PackratParser[String] = keyword("FALSE")
  lazy val SLCT: PackratParser[String] = keyword("SLCT")
  lazy val TABLE: PackratParser[String] = keyword("TABLE")
  lazy val ABS: PackratParser[String] = keyword("ABS")
  lazy val NOT: PackratParser[String] = 
    (keyword("NOT") | lexeme(regex("""~(?![\=])""".r))) ! 
      "expecting keyword 'NOT' or operator '~'"
  lazy val OF: PackratParser[String] = keyword("OF")
  lazy val MOD: PackratParser[String] = 
    (keyword("MOD") | keyword("REM")) ! "expecting keyword 'MOD' or 'REM'"
  lazy val EQV: PackratParser[String] = keyword("EQV")
  lazy val XOR: PackratParser[String] = 
    (keyword("XOR") | keyword("NEQV")) ! "expecting keyword 'NEQV' or 'XOR'"
  lazy val VALOF: PackratParser[String] = keyword("VALOF")
  lazy val RESULTIS: PackratParser[String] = keyword("RESULTIS")
  lazy val GLOBAL: PackratParser[String] = keyword("GLOBAL")
  lazy val STATIC: PackratParser[String] = keyword("STATIC")
  lazy val MANIFEST: PackratParser[String] = keyword("MANIFEST")
  lazy val IF: PackratParser[String] = keyword("IF")
  lazy val UNLESS: PackratParser[String] = keyword("UNLESS")
  lazy val TEST: PackratParser[String] = keyword("TEST")
  lazy val BE: PackratParser[String] = keyword("BE")
  lazy val ELSE: PackratParser[String] = keyword("ELSE")
  lazy val WHILE: PackratParser[String] = keyword("WHILE")
  lazy val UNTIL: PackratParser[String] = keyword("UNTIL")
  lazy val REPEAT: PackratParser[String] = keyword("REPEAT")
  lazy val REPEATWHILE: PackratParser[String] = keyword("REPEATWHILE")
  lazy val REPEATUNTIL: PackratParser[String] = keyword("REPEATUNTIL")
  lazy val FOR: PackratParser[String] = keyword("FOR")
  lazy val TO: PackratParser[String] = keyword("TO")
  lazy val BY: PackratParser[String] = keyword("BY")
  lazy val SWITCHON: PackratParser[String] = keyword("SWITCHON")
  lazy val INTO: PackratParser[String] = keyword("INTO")
  lazy val CASE: PackratParser[String] = keyword("CASE")
  lazy val DEFAULT: PackratParser[String] = keyword("DEFAULT")
  lazy val RETURN: PackratParser[String] = keyword("RETURN")
  lazy val ENDCASE: PackratParser[String] = keyword("ENDCASE")
  lazy val LOOP: PackratParser[String] = keyword("LOOP")
  lazy val BREAK: PackratParser[String] = keyword("BREAK")
  lazy val GOTO: PackratParser[String] = keyword("GOTO")
  lazy val FINISH: PackratParser[String] = keyword("FINISH")
  lazy val LET: PackratParser[String] = keyword("LET")
  lazy val AND: PackratParser[String] = keyword("AND")
  lazy val VEC: PackratParser[String] = keyword("VEC")

  // These parsers for 'DO' and 'THEN' implement the "may be omitted whenever
  // they are followed by a command keyword" rule.
  lazy val DO: PackratParser[String] =
    lexeme(keyword("DO") | guard(keywordBCPL)) ! "expecting 'DO' or keyword"
  lazy val THEN: PackratParser[String] =
    lexeme(keyword("THEN") | guard(keywordBCPL)) ! "exprecting 'THEN' or keyword"

  /**
    * Make a regular expression from a word list that matches
    * the longest prefix of a string equal to a word in that list.
    */
  def wordListMatcher(ls: Seq[String]): String = {
    (ls.distinct.filter(_ != "")) match {
      case Nil => ""
      case fs =>
        fs.groupBy(s => s.charAt(0))
          .map {
            case (c, ls) =>
              """\Q""" ++ c.toString ++ """\E""" ++
                wordListMatcher(ls.map(_.substring(1)))
          }
          .mkString("(", "|", ")") ++
          (if (ls.contains("")) "?" else "")
    }
  }
}
