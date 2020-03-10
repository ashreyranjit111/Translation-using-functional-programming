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
// This code implements the BCPL `writef` function, which evolved into the
// `printf` function in the C standard library. It is taken from the original 
// BCPL standard library written by Martin Richards.
//
// As this stands this code will only work with those weBCPL strings that
// confine themselves to the ASCII portion of the UTF-8 encoding.
//

LET writed(n, d) BE
$(
    LET t = VEC 30
    AND i, k = 0, -n
    IF n<0 DO d, k := d-1, n
    t!i, i, k := -(k REM 10), i+1, k/10 REPEATUNTIL k=0
    FOR j = i+1 TO d DO wrch('*s')
    IF n<0 DO wrch('-')
    FOR j = i-1 TO 0 BY -1 DO wrch(t!j+'0')
$)

AND writeu(n, d) BE
$(
    LET m = (n>>1)/5
    UNLESS m=0 DO $( writed(m, d-1); d := 1 $)
    writed(n-m*10, d)
$)

AND writen(n) BE writed(n, 0)

AND writebin(n, d) BE
$(
    IF d>1 DO writebin(n>>1, d-1)
    wrch((n&1)+'0')
$)

AND writeoct(n, d) BE
$(
    IF d>1 DO writeoct(n>>3, d-1)
    wrch((n&7)+'0')
$)

AND writehex(n, d) BE
$(
    IF d>1 DO writehex(n>>4, d-1)
    wrch((TABLE '0','1','2','3','4','5','6','7',
                '8','9','A','B','C','D','E','F')!(n&15) )
$)

AND writes(s) BE FOR i = 1 TO s%0 DO wrch(s%i)

AND writet(s, d) BE
$(
    writes(s)
    FOR i = 1 TO d-s%0 DO wrch('*s')
$)

AND writef(format, a, b, c, d, e, f, g, h, i, j, k) BE
$(
    LET t = @ a
    FOR p = 1 TO format%0 DO
    $(
        LET k = format%p
        TEST k='%' THEN 
        $(
            LET f, n = ?, ?
            p := p+1
            SWITCHON capitalch(format%p) INTO
            $(
                DEFAULT:  wrch(format%p); ENDCASE
                CASE 'S': f := writes;    GOTO l
                CASE 'T': f := writet;    GOTO m
                CASE 'C': f := wrch;      GOTO l
                CASE 'B': f := writebin;  GOTO m
                CASE 'O': f := writeoct;  GOTO m
                CASE 'X': f := writehex;  GOTO m
                CASE 'I': f := writed;    GOTO m
                CASE 'N': f := writen;    GOTO l
                CASE 'U': f := writeu
                m:        p := p+1
                          n := format%p
                          n := '0'<=n<='9' -> n-'0', 10+n-'A'
                l:        f(!t, n)
                CASE '$': t := t+1
            $)
        $)
        ELSE wrch(k)
    $)
$)
