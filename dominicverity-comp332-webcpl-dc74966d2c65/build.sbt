/*
 * This file is part of COMP332 Assignment 2/3 2019.
 * 
 * WebBCPL, a retro language compiler. Simple BCPL to WebAssembly compiler.
 *
 * Copyright (C) 2019 Dominic Verity, Macquarie University.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

ThisBuild/organization := "comp.mq.edu.au"
ThisBuild/scalaVersion := "2.12.8"
ThisBuild/scalacOptions :=
    Seq(
        "-deprecation",
        "-feature",
        "-unchecked",
        "-Xcheckinit",
        "-Xfatal-warnings",
        "-Xlint:-stars-align,_"
    )

// Settings for the Assignment 2/3 project.
lazy val weBCPL = (project in file("."))
  .settings(
    // Project information
    name := "BCPL to WebAssembly Compiler",
    version := "0.1",

    // Execution
    parallelExecution in Test := false,

    // Dependencies
    libraryDependencies ++=
      Seq (
        "org.bitbucket.inkytonik.kiama" %% "kiama" %
          "2.2.1" withSources() withJavadoc(),
        "org.bitbucket.inkytonik.kiama" %% "kiama-extras" % "2.2.1" %
          "test" classifier ("tests"),
        "junit" % "junit" % "4.12" % "test",
        "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
        "org.scalatest" %% "scalatest" % "3.0.8" % "test"
      )
  )

// Interactive settings

logLevel := Level.Info

shellPrompt := {
    state =>
        Project.extract(state).currentRef.project + " " + version.value +
            " " + scalaVersion.value + "> "
}


