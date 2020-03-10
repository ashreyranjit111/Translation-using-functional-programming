/*
 * This file is part of COMP332 Assignment 2/3 2019.
 *
 * weBCPL, a retro BCPL to WebAssembly compiler.
 *
 * © 2019, Dominic Verity, Macquarie University.
 *         All rights reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Source program tree definition.
 */

package webcpl

/**
  * Module containing tree structures for representing BCPL programs.
  */
object BCPLTree {

  import org.bitbucket.inkytonik.kiama.relation.Tree

  /**
    * A relational tree to handle access to parent and sibling nodes.
    */
  type SourceTree = Tree[SourceNode, Program]

  /**
    * The common supertype of all source tree nodes.
    */
  sealed abstract class SourceNode extends Product

  /**
    * A BCPL program is a list of declarations.
    */
  case class Program(exps: Vector[Declaration]) extends SourceNode

  /**
    * Common superclass of declarations
    */
  sealed abstract class Declaration extends SourceNode

  /**
    * A `GLOBAL` declaration introduces identifiers that refer to entries in
    * the globals table. This table is shared between the modules in a linked
    * executable.
    */
  case class GlobalDecl(decls: Vector[GlobalEntry]) extends Declaration

  /**
    * An entry in a `GLOBAL` declaration block.
    */
  case class GlobalEntry(name: IdnDef, loc: Option[IntExp]) extends Declaration

  /**
    * A `STATIC` declaration introduces variables that contain values shared
    * between the functions in a single module.
    */
  case class StaticDecl(decls: Vector[StaticEntry]) extends Declaration

  /**
    * An entry in a `STATIC` declaration block.
    */
  case class StaticEntry(name: IdnDef, exp: Option[Expression]) extends Declaration

  /**
    * A `MANIFEST` declaration introduces identifiers that are bound to
    * constants.
    */
  case class ManifestDecl(decls: Vector[ManifestEntry]) extends Declaration

  /**
    * An entry in a `MANIFEST` declaration block.
    */
  case class ManifestEntry(name: IdnDef, exp: Option[Expression]) extends Declaration

  /**
    * A `LET..AND...AND...` declaration which introduces new procedures and
    * functions.
    */
  case class LetDecl(cls: Vector[LetClause]) extends Declaration

  /**
    * Common superclass for let declaration clauses.
    */
  sealed abstract class LetClause extends SourceNode

  /**
    * Common superclass for procedure and function declarations
    */
  sealed abstract class LetRoutineClause extends LetClause {
    def idn: IdnDef
    def params: Vector[IdnDef]
  }

  /**
    * A `LET...BE...` declaration clause, which introduces a procedure declaration.
    */
  case class LetProcClause(idn: IdnDef, params: Vector[IdnDef], body: Statement)
      extends LetRoutineClause

  /**
    * A `LET...=...` declaration clause, which introduces a function declaration.
    */
  case class LetFnClause(idn: IdnDef, params: Vector[IdnDef], body: Expression)
      extends LetRoutineClause

  /**
    * A `LET` declaration that introduces new local variables and binds them
    * to the result of evaluating corresponding initialisation expressions.
    */
  case class LetVarClause(idns: Vector[IdnDef], exps: Vector[Expression])
    extends LetClause

  /**
    * A `LET` declaration that introduces and allocates a new local vector of
    * size that is fixed at compile time. The vector allocated in the stack has
    * size given by a constant expression `exp` plus 1.
    */
  case class LetVecClause(idn: IdnDef, exp: Expression) extends LetClause

  /**
    * Common superclass of statements
    */
  sealed abstract class Statement extends SourceNode

  /**
    * Assignment statement (`:=`).
    */
  case class AssignStmt(lval: Vector[Expression], rval: Vector[Expression])
      extends Statement

  /**
    * Wrap a procedure or function call expression to use it as a procedure
    * call statement.
    */
  case class CallStmt(call: CallExp) extends Statement

  /*
   * In BCPL we use different keywords to distinguish variants with a `THEN` clause only,
   * an `ELSE` clause only, and both branches:
   *     IF...DO...
   *     UNLESS...DO...
   *     TEST...THEN...ELSE...
   * For the purposes of conditionals, `0` is interpreted as `FALSE` and all non-`0`
   * numbers as `TRUE`.
   */
  
  /**
    * An 'IF...DO...' statement.
    */ 
  case class IfDoStmt(exp: Expression, thn: Statement) extends Statement
  
  /**
    * An 'UNLESS...DO...' statement. 
    */
  case class UnlessDoStmt(exp: Expression, els: Statement) extends Statement

  /**
    * A 'TEST...THEN...ELSE...' statement.
    */ 
  case class TestThenElseStmt(exp: Expression, thn: Statement, els: Statement)
      extends Statement

  /**
    * A 'WHILE...DO...' pre-body test loop.
    */
  case class WhileDoStmt(exp: Expression, body: Statement) extends Statement

  /**
    * An 'UNTIL...DO...' pre-body test loop.
    */
  case class UntilDoStmt(exp: Expression, body: Statement) extends Statement
  
  /**
    * A '...REPEAT' loop. Infinite loop, can be broken out of by 'BREAK'.
    */ 
  case class RepeatStmt(body: Statement) extends Statement

  /**
    * A '...REPEATWHILE...' post-body test loop.
    */
  case class RepeatWhileStmt(body: Statement, exp: Expression) extends Statement

  /**
    * A '...REPEATUNTIL...' post-body test loop.
    */
  case class RepeatUntilStmt(body: Statement, exp: Expression) extends Statement

  /**
    * A `FOR` loop. BCPL, unlike C, has genuine "Pascal-style" for loops.
    */
  case class ForStmt(
      idn: IdnDef,
      start: Expression,
      end: Expression,
      step: Option[Expression],
      body: Statement
  ) extends Statement

  /**
    * A `SWITCHON` statement. This is BCPL's equivalent of the C `switch` statement.
    */
  case class SwitchOnStmt(exp: Expression, cases: Block) extends Statement

  /*
   * Control flow statements for jumping out of loops, returning from functions
   * and procedures and so forth. Includes the much vilified `GOTO` statement.
   */

  /**
    * A `RESULTIS` statement, returns a value from the nearest enclosing `VALOF`
    * block.
    */
  case class ResultIsStmt(exp: Expression) extends Statement

  /**
    * A `RETURN` statement, causes the current routine to terminate and return
    * control to its call site.
    */
  case class ReturnStmt() extends Statement

  /**
    * An `ENDCASE` statement, escape from the nearest enclosing `SWITCHON`
    * statement.
    */
  case class EndCaseStmt() extends Statement

  /**
    * A `LOOP` statement, jump to the **end of the body** of the nearest enclosing
    * iterative command (`REPEAT` etc) and continue on to execution of next iteration.
    */
  case class LoopStmt() extends Statement

  /**
    * A `BREAK` statement, jump to the point **just after** the nearest enclosing
    * iterative command.
    */
  case class BreakStmt() extends Statement

  /**
    * A `FINISH` statement, terminate the execution of the executing program.
    */
  case class FinishStmt() extends Statement

  /**
    * A `GOTO` statement, jump to a labelled command which must be in the body of
    * the current function or procedure.
    */
  case class GotoStmt(lab: LabUse) extends Statement

  /**
    * Attach a named label to a statement.
    */
  case class Labelled(idn: LabDef, stmt: Statement) extends Statement

  /**
    * Attach the 'DEFAULT' case label to a statement.
    */
  case class Default(stmt: Statement) extends Statement

  /**
    * Attach a `CASE` label to a statement.
    */
  case class CaseOf(exp: Expression, stmt: Statement) extends Statement

  /**
    * A block comprising a (possibly empty) list of declarations followed by
    * a list of statements. A block containing no statements is called a
    * *compound statement*
    */
  case class Block(decls: Vector[Declaration], stmts: Vector[Statement])
      extends Statement

  /**
    * Common superclass of expressions.
    */
  sealed abstract class Expression extends SourceNode

  /**
    * A call to a procedure or function in expression position.
    */
  case class CallExp(exp: Expression, args: Vector[Expression])
      extends Expression

  /**
    * A `VALOF` expression, executes a command in expression position and
    * evaluate to the value produced by the first `RESULTIS` statement
    * executed in that command. The result returned is undefined if the
    * command terminates before a `RESULTIS` is executed.
    */
  case class ValofExp(stmt: Statement) extends Expression

  /**
    * Integer constant expression. Can be specified in binary, octal or
    * hexadecimal notation using the prefixes `#b`, `#o` and `#x`.
    */
  case class IntExp(value: Int) extends Expression

  /**
    * The `TRUE` expression, a builtin constant equal to `-1`.
    */
  case class TrueExp() extends Expression

  /**
    * The `FALSE` expression, a builtin constant equal to `0`.
    */
  case class FalseExp() extends Expression

  /**
    * The undefined expression `?`. This is a value about which we can make
    * no assumptions, except that it is a number. For example, the expression `? = ?`
    * may not even evaluate to `TRUE`! This is usually used in situations like
    * initialisation expressions and arguments to procedure / function calls where
    * we want to pass a number whose precise value is unimportant. In principle,
    * this might allow us to optimise things by eliminating some initialisation code.
    */
  case class UndefExp() extends Expression

  /**
    * A character constant expression. Represents a single, fixed UTF8 character.
    */
  case class ChrExp(ch: Vector[Int]) extends Expression

  /**
    * A string constant expression. Represents a "packed" UTF8 string.
    */
  case class StringExp(str: Vector[Int]) extends Expression

  /**
    * A table constant expression. Represents a fixed table of word values.
    */
  case class TableExp(exps: Vector[Expression]) extends Expression

  /**
    * Common superclass of expressions that can act as l-values in assignments.
    */
  sealed abstract class LValue extends Expression

  /**
    * Named variable expression.
    */
  case class IdnExp(name: IdnUse) extends LValue

  /**
    * A unary pling (`!`) expression. Dereferences a pointer and returns the
    * word stored at the location pointed to.
    */
  case class UnaryPlingExp(ptr: Expression) extends LValue

  /**
    * A binary pling (`!`) expression. Add a word offset to a pointer and
    * dereference to return the word stored at the computed location.
    */
  case class BinaryPlingExp(ptr: Expression, off: Expression) extends LValue

  /**
    * A unary byte pling (`%`) expression. Dereferences a pointer and returns the
    * byte stored at the location pointed to.
    */
  case class UnaryBytePlingExp(ptr: Expression) extends LValue

    /**
    * A binary byte pling (`%`) expression. Add a byte offset to a pointer and
    * dereference to return the unsigned byte stored at the computed location
    * (padded to a word with 0s).
    */
  case class BinaryBytePlingExp(ptr: Expression, off: Expression) extends LValue

  /*
   * Remaining expressions are r-values. Unary operators first.
   */

  /**
    * Address-of operation (`@`), returns the address of a variable (static or local)
    * or of a dereferenced value (a pling expression).
    */
  case class AddrOfExp(exp: Expression) extends Expression

  /**
    * Integer (unary) negation `-`.
    */
  case class NegExp(exp: Expression) extends Expression

  /**
    * Bitwise not operator `NOT` or `~`.
    */
  case class NotExp(exp: Expression) extends Expression

  /**
    * Absolute value operator.
    */
  case class AbsExp(exp: Expression) extends Expression

  /*
   * Binary operators.
   */

  /**
    * Shift left `<<`
    */
  case class ShiftLeftExp(exp: Expression, shft: Expression) extends Expression

  /**
    * Shift left `<<`
    */
  case class ShiftRightExp(shft: Expression, exp: Expression) extends Expression

  /**
    * Plus expression. `+`
    */
  case class PlusExp(left: Expression, right: Expression) extends Expression

  /**
    * Minus expression `-`.
    */
  case class MinusExp(left: Expression, right: Expression) extends Expression

  /**
    * Multiplication expression `*`.
    */
  case class StarExp(left: Expression, right: Expression) extends Expression

  /**
    * Division expression `/`.
    */
  case class SlashExp(left: Expression, right: Expression) extends Expression

  /**
    * Modulus expression `MOD` or `REM`.
    */
  case class ModExp(left: Expression, right: Expression) extends Expression

  /**
    * Bitwise equal expression `EQV`.
    */
  case class EqvExp(left: Expression, right: Expression) extends Expression

  /**
    * Bitwise not equal expression `XOR` or `NEQV`.
    */
  case class XorExp(left: Expression, right: Expression) extends Expression

  /*
   * In BCPL the `NOT`, `&` (and) and `|` (or) operators have a semantics that
   * differs depending upon the position of the expression they appear in.
   * The control expressions of if statements / expression and conditional loops
   * are said to be in "boolean context", and in such a context these operators
   * enjoy short-circuited evaluation. In all other positions they are evaluated
   * as bitwise operators.
   */

  /**
    * And expression `&`.
    */
  case class AndExp(left: Expression, right: Expression) extends Expression

  /**
    * Or expression `|`.
    */
  case class OrExp(left: Expression, right: Expression) extends Expression

  /*
   * In BCPL we can write relational expressions like `a <= b < c > d` which
   * in other languages would be written as `a <= b & b < c & c > d`.
   */

  /**
    * Equality relation `=`.
    */
  case class EqualExp(left: Expression, right: Expression)
      extends Expression

  /**
    * Inequality relation `~=`.
    */
  case class NotEqualExp(left: Expression, right: Expression)
      extends Expression

  /**
    * Less than or equal relation `<=`.
    */
  case class LessOrEqualExp(left: Expression, right: Expression)
      extends Expression

  /**
    * Greater than or equal relation `>=`.
    */
  case class GreaterOrEqualExp(left: Expression, right: Expression)
      extends Expression

  /**
    * Less than relation `<`.
    */
  case class LessExp(left: Expression, right: Expression) extends Expression

  /**
    * Greater than relation `>`.
    */
  case class GreaterExp(left: Expression, right: Expression)
      extends Expression

  /**
    * If expresion `E1 -> E2, E3`, evaluate `E1` if it is `FALSE` evaluate and
    * return `E3` otherwise evaluate and return `E2`.
    */
  case class IfExp(exp: Expression, thn: Expression, els: Expression)
      extends Expression

  /**
    * Abstract superclass for identifier references.
    */
  sealed abstract class IdnNode extends SourceNode {
    def idn: String
  }

  /**
    * A defining occurrence of an identifier (i.e., a place where an entity named by
    * the identifier is being introduced).
    */
  case class IdnDef(idn: Identifier) extends IdnNode

  /**
    * An applied occurrence (use) of an identifier (i.e., a place where an entity with
    * this name is being used, but not introduced).
    */
  case class IdnUse(idn: Identifier) extends IdnNode

  /**
    * A defining occurrence of a label (i.e., a named target for a `GOTO` jump).
    * Labels and variable identifiers occupy separate namespaces.
    */
  case class LabDef(idn: Identifier) extends IdnNode

  /**
    * An applied occurrence (use) of a label.
    */
  case class LabUse(idn: Identifier) extends IdnNode

  /**
    * A representation of identifiers as strings.
    */
  type Identifier = String

}
