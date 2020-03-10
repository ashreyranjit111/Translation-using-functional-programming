//
// This file is part of COMP332 Assignment 2/3 2019.
//
// weBCPL, a retro BCPL to WebAssembly compiler.
//
// © 2019, Dominic Verity and Anthony Sloane, Macquarie University.
//         All rights reserved.
// © 2005, Martin Richards
//
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// Random number generator, adapted from example given in the BCPL manual.
// It is based upon an algorithm given in Knuth: The art of programming, 
// vol 2, p 26.
//

MANIFEST { 
    SIZE = 55 
    MASK = #x_FFF_FFFF
    MAGIC_1 = #x_234_5678
    MAGIC_2 = #x_536_2781
}

LET rnd(n) = VALOF 
{   
    LET val = (ranv!rani + ranv!ranj) & MASK
    ranv!rani := val
    rani := (rani + 1) MOD SIZE
    ranj := (ranj + 1) MOD SIZE
    RESULTIS val MOD n
}

AND initrnd(seed) = VALOF 
{   
    LET a, b = MAGIC_1 + seed, MAGIC_2
    FOR i = 0 TO (SIZE - 1) DO
    { 
        LET t = (a+b) & MASK
        a := b
        b := t
        ranv!i := t
    }
    rani, ranj := 0, 31
    RESULTIS TRUE
}

AND ranv = VEC (SIZE - 1)