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
 * Parser for the BCPL language.
 */

package webcpl

import org.bitbucket.inkytonik.kiama.parsing.Parsers
import org.bitbucket.inkytonik.kiama.util.Positions

/**
  * Module containing parsers for BCPL.
  */
class SyntaxAnalysis(positions: Positions)
    extends Parsers(positions)
    with LexicalAnalysis {

  import BCPLTree._

  lazy val parser: PackratParser[Program] =
    phrase(program)

  lazy val program: PackratParser[Program] =
    rep1sep(declaration, semiColon) ^^ Program

  lazy val declaration: PackratParser[Declaration] =
    MANIFEST ~> leftBrace ~> rep1sep(manifestEntry, semiColon) <~ rightBrace ^^ ManifestDecl |
      GLOBAL ~> leftBrace ~> rep1sep(globalEntry, semiColon) <~ rightBrace ^^ GlobalDecl |
      STATIC ~> leftBrace ~> rep1sep(staticEntry, semiColon) <~ rightBrace ^^ StaticDecl |
      LET ~> rep1sep(letDeclClause, AND) ^^ LetDecl

  lazy val manifestEntry: PackratParser[ManifestEntry] =
    idndef ~ opt(equal ~> expression) ^^ ManifestEntry

  lazy val staticEntry: PackratParser[StaticEntry] =
    idndef ~ opt(equal ~> expression) ^^ StaticEntry

  lazy val globalEntry: PackratParser[GlobalEntry] =
    idndef ~ opt(colon ~> (integerConst ^^ IntExp)) ^^ GlobalEntry

  lazy val letDeclClause: PackratParser[LetClause] =
    rep1sep(idndef, comma) ~ (equal ~> rep1sep(expression, comma)) ^^ LetVarClause |
      idndef ~ (equal ~> VEC ~> expression) ^^ LetVecClause |
      idndef ~ (leftParen ~> repsep(idndef, comma) <~ rightParen) ~ (equal ~> expression) ^^ LetFnClause |
      idndef ~ (leftParen ~> repsep(idndef, comma) <~ rightParen) ~ (BE ~> statement) ^^ LetProcClause

  /*
   * Statement parsers.
   */

  lazy val statement: PackratParser[Statement] =
    (labdef <~ colon) ~ statement ^^ Labelled |
      (CASE ~> expression <~ colon) ~ statement ^^ CaseOf |
      DEFAULT ~> colon ~> statement ^^ Default |
      unlabelledStmt

  // FIXME Replace this stubbed parser.
  lazy val unlabelledStmt: PackratParser[Statement] =
    repeatableStmt | iteratedStmt | testStmt

  // FIXME Add your parsers for weBCPL statements here.
  lazy val iteratedStmt: PackratParser[Statement] = {
    UNTIL ~ expression ~ DO ~ statement ^^ { case a ~ b ~ c ~ d => UntilDoStmt(b, d) } |
      WHILE ~ expression ~ DO ~ statement ^^ { case a ~ b ~ c ~ d => WhileDoStmt(b, d) } |
      FOR ~ idndef ~ equal ~ expression ~ TO ~ expression ~ 
      opt(BY ~> expression) ~ DO ~ statement ^^ { case a ~ b ~ c ~ d ~ e ~ f ~ g ~ h ~ i => ForStmt(b, d, f, g, i) }
  }

  lazy val testStmt: PackratParser[Statement] = {
    TEST ~ expression ~ THEN ~ statement ~ ELSE ~ statement ^^ { case a ~ b ~ c ~ d ~ e ~ f => TestThenElseStmt(b, d, f) } |
      IF ~ expression ~ DO ~ statement ^^ { case a ~ b ~ c ~ d => IfDoStmt(b, d) } |
      UNLESS ~ expression ~ DO ~ statement ^^ { case a ~ b ~ c ~d => UnlessDoStmt(b, d) } 
  }

  lazy val repeatableStmt: PackratParser[Statement] =
    repeatableStmt ~ REPEAT ^^ { case a ~ b => RepeatStmt(a) } |
      repeatableStmt ~ REPEATWHILE ~ expression ^^ { case a ~ b ~ c => RepeatWhileStmt(a, c) } |
      repeatableStmt ~ REPEATUNTIL ~ expression ^^ { case a ~ b ~ c => RepeatUntilStmt(a, c) } |
      simpleStmt


  lazy val simpleStmt: PackratParser[Statement] = 
    rep1sep(expression, comma) ~ assign ~ rep1sep(expression, comma) ^^ { case a ~ b ~ c => AssignStmt(a, c) } | 
      callExp ^^ { case a => CallStmt(a) } | BREAK ^^^ BreakStmt() | LOOP ^^^ LoopStmt() | ENDCASE ^^^ EndCaseStmt() | 
      RETURN ^^^ ReturnStmt() | FINISH ^^^ FinishStmt() |GOTO ~ labuse ^^ { case a ~ b => GotoStmt(b) } | 
      RESULTIS ~ expression ^^ { case a ~ b => ResultIsStmt(b) } | 
      SWITCHON ~ expression ~ INTO ~ blockStmt ^^ { case a ~ b ~ c ~ d => SwitchOnStmt(b, d) } |
      blockStmt

  lazy val blockStmt: PackratParser[Block] = 
    leftBrace ~> repsep(declaration, semiColon) ~ rep1sep(statement, semiColon) <~ rightBrace ^^ Block

  /*
   * Expression parsers.
   */

  /**
    * Top level expression parser, parse `VALOF` and `TABLE` expressions.
    */
  lazy val expression: PackratParser[Expression] =
    VALOF ~> statement ^^ ValofExp |
      TABLE ~> rep1sep(expression, comma) ^^ TableExp |
      condExp

  /**
    * Level 1, parse if expressions `->`.
    */
  // FIXME Add your expression parsers for levels 1-6 of the precedence hierarchy here.
  lazy val condExp: PackratParser[Expression] = 
    level_2 ~ rightArrow ~ level_2 ~ comma ~ condExp ^^ { case a ~ b ~ c ~ d ~ e => IfExp(a, c, e) } |
      level_2
      
  lazy val level_2: PackratParser[Expression] =
    level_2 ~ EQV ~ level_3 ^^ { case a ~ b ~ c => EqvExp(a, c) } |
      level_2 ~ XOR ~ level_3 ^^ { case a ~ b ~ c => XorExp(a, c) } |
      level_3

  lazy val level_3: PackratParser[Expression] =
    level_3 ~ pipe ~ level_4 ^^ { case a ~ b ~ c => OrExp(a, c) } |
      level_4

  lazy val level_4: PackratParser[Expression] =
    level_4 ~ apersand ~ level_5 ^^ { case a ~ b ~ c => AndExp(a, c) } |
      level_5
  
  lazy val level_5: PackratParser[Expression] =
    NOT ~ level_5 ^^ { case a ~ b => NotExp(b) } |
      level_6

  lazy val level_6: PackratParser[Expression] =
    level_6 ~ shiftLeft ~ relExp ^^ { case a ~ b ~ c => ShiftLeftExp(a, c) } |
      level_6 ~ shiftRight ~ relExp ^^ { case a ~ b ~ c => ShiftRightExp(a, c) } |
      relExp

  /**
    * Level 7, parse relational expressions `~=`, `=`, `>=`, `<=`...
    *
    * This is slightly nonstandard because in BCPL we can write relational
    * expressions like `a <= b < c > d` which in other languages might be
    * written as `a <= b & b < c & c > d`.
    */
  lazy val relExp: PackratParser[Expression] =
    rep1(
      addExp ~
        (notEqual ^^^ NotEqualExp |
          lessOrEqual ^^^ LessOrEqualExp |
          greaterOrEqual ^^^ GreaterOrEqualExp |
          equal ^^^ EqualExp |
          less ^^^ LessExp |
          greater ^^^ GreaterExp)
    ) ~ addExp ^^ {
      case v ~ t =>
        (v zip (v.tail.map(_._1) :+ t))
          .map { case ((l ~ rel), r) => rel(l, r) }
          .reduceLeft(AndExp)
    } | addExp

  /**
    * Level 8, parse additive operator expressions, that is those involving
    * binary `-` and `+`.
    */
  // FIXME Replace this stubbed parser
  lazy val addExp: PackratParser[Expression] = 
    addExp ~ minus ~ level_9 ^^ { case a ~ b ~ c => MinusExp(a, c) } |
      addExp ~ plus ~ level_9 ^^ { case a ~ b ~ c => PlusExp(a, c) } |
      level_9

  // FIXME Add your expression parsers for levels 8-12 of the precedence hierarchy here.
  lazy val level_9: PackratParser[Expression] =
    unaryMinus ~ level_9 ^^ { case a ~ b => NegExp(b) } |
      unaryPlus ~ level_9 ^^ { case a ~ b => b } |
      ABS ~ level_9 ^^ { case a ~ b => AbsExp(b) } |
      level_10

  lazy val level_10: PackratParser[Expression] =
    level_10 ~ star ~ level_11 ^^ { case a ~ b ~ c => StarExp(a,c) } |
      level_10 ~ slash ~ level_11 ^^ { case a ~ b ~ c => SlashExp(a, c) } |
      level_10 ~ MOD ~ level_11 ^^ { case a ~ b ~ c => ModExp(a, c) } |
      level_11

  lazy val level_11: PackratParser[Expression] =
    unaryPling ~ level_11 ^^ { case a ~ b => UnaryPlingExp(b) } |
      unaryPercent ~ level_11 ^^ { case a ~ b => UnaryBytePlingExp(b) } |
      at ~ level_11 ^^ { case a ~ b => AddrOfExp(b) } |
      level_12

  lazy val level_12: PackratParser[Expression] =
      level_12 ~ pling ~ primaryExp ^^ { case a ~ b ~ c => BinaryPlingExp(a, c) } |
      level_12 ~ percent ~ primaryExp ^^ { case a ~ b ~ c => BinaryBytePlingExp(a, c) } |
      primaryExp

  /**
    * Level 13, parse primary expressions, that is function calls, identifiers,
    * bracketed expressions, and literal constants.
    */
  lazy val primaryExp: PackratParser[Expression] =
    callExp | elemExp

  lazy val callExp: PackratParser[CallExp] =
    (callExp | elemExp) ~ (leftParen ~> repsep(expression, comma) <~ rightParen) ^^ CallExp

  // PRO TIP: Place parsers that match longer initial segments earlier in an alternation.

  /*
   * If two clauses of an alternation `|` can match the same text, then place the one
   * that matches longer initial segments first. This ensures that the longest possible
   * match is preferred.
   */

  lazy val elemExp: PackratParser[Expression] =
    leftParen ~> expression <~ rightParen |
      TRUE ^^^ TrueExp() |
      FALSE ^^^ FalseExp() |
      question ^^^ UndefExp() |
      integerConst ^^ IntExp |
      charConst ^^ ChrExp |
      stringConst ^^ StringExp |
      idnuse ^^ IdnExp

  lazy val idndef: PackratParser[IdnDef] =
    identifier ^^ IdnDef

  lazy val idnuse: PackratParser[IdnUse] =
    identifier ^^ IdnUse

  lazy val labdef: PackratParser[LabDef] =
    identifier ^^ LabDef

  lazy val labuse: PackratParser[LabUse] =
    identifier ^^ LabUse
}
