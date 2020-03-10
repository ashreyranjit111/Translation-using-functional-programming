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
// Sample sorting code from Martin Richards BCPL distribution.
//

MANIFEST $( upb=60; big=1000  $)

LET start() BE
$(
    LET p, q = 1, 0
    LET v = VEC upb
    AND w = VEC upb
    FOR i = 1 TO upb-1 DO v!i := 'A' + randno(26) - 1
    v!0, v!upb := big, big
    $(
        q := 0
        FOR i = 1 TO p-1 DO wrch(v!i)
        wrch('|')
        FOR i = p TO upb-1 DO $( q:=q+1; w!q := i; wrch(v!i) $)
        newline()
        q := q+1
        UNTIL q<=2 DO
        $(
            LET r = 0
            w!q := upb
            FOR i = 1 TO q-1 DO
                IF v!(w!(i-1))>v!(w!i)<=v!(w!(i+1)) DO $( r:=r+1; w!r:=w!i $)
            q := r+1
            w!q := upb
            FOR i = 1 TO p-1 DO wrch(v!i)
            wrch('|')
            r := 1
            FOR i = p TO upb-1 TEST w!r=i
                               THEN $( wrch(v!i); r := r+1 $)
                               ELSE wrch(' ')
            newline()
        $)
        q := v!p
        v!p := v!(w!1)
        v!(w!1) := q
        p := p+1
    $) REPEATUNTIL p>=upb
$)