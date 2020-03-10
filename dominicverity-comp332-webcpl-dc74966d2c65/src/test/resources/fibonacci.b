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
// A simple iterative Fibonacci implementation in BCPL.
//

GLOBAL { 
    start:  1 
    writef: 94
}

LET Fibonacci(n) = VALOF 
{
    LET res1, res2, temp = 0, 1, ?

    WHILE 0 < n DO {
        temp := res1
        res1 := res2
        res2 := temp + res1
        n := n - 1
    }

  RESULTIS res1
}

LET start() BE 
{
    writef(
        "The 10th Fibonacci number is %n*n", 
        fibonacci(n))
}

