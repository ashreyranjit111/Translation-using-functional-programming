//
// This file is part of COMP332 Assignment 2/3 2019.
//
// weBCPL, a retro BCPL to WebAssembly compiler.
//
// © 2019, Dominic Verity and Anthony Sloane, Macquarie University.
//         All rights reserved.
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// BCPL expression examples, to illustrate the precedence and
// associativity rules of the language.
//

GLOBAL {
    start : 1
}

LET x, y, i, j = ?, ?, 3, 5

AND v = VEC 49

AND start() = VALOF {
    LET a = ! @ x
    AND b = ! v ! i ! j
    AND c = @ v ! i ! j
    AND d = x << 1+y >> 1
    AND e = ~ x ! y
    AND f = ~ x = y
    AND g = NOT x = y
    AND h = - 10 * a / b + c - -10 + d * e
    AND k = FALSE XOR f & g | a & h | c EQV i + j
    AND l = v!i + v!j - a * b

    RESULTIS a -> b, c -> d, e
}