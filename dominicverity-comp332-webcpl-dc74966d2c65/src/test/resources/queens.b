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
// Solution to the n-Queens problem from Martin Richards BCPL distribution.
//

GLOBAL 
$(
	count: 200
	all: 201
$)

LET try(ld, row, rd) BE
	TEST row = all THEN
		count := count + 1
	ELSE
    $(
		LET poss = all & ~(ld | row | rd)
		UNTIL poss = 0 DO
        $(
			LET p = poss & -poss
			poss := poss - p
			try(ld + p << 1, row + p, rd + p >> 1)
		$)
	$)

LET start() = VALOF
$(
	all := 1
	FOR i = 1 TO 12 DO
    $(
		count := 0
		try(0, 0, 0)
		writef("%i2-Queens problem has %i5 solutions*n", i, count)
		all := 2 * all + 1
	$)
	RESULTIS 0
$)
