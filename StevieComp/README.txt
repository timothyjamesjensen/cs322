This folder contains a compiler for Stevie that generates code for
a (slightly modified version of) the DemoComp Target language.

This program may be compiled using the supplied Makefile by typing the
command:

  make

(You can also use "make clean" to remove the compiled files.)

To use the resulting compiler, use a command of the following form:

  java StevieComp < sourcefile.stevie

where sourcefile.stevie contains the source code that you want to
compile.  For valid inputs, this command will:

- Parse the input file

- Display a pretty-printed version of the source code

- Run static analysis on the source program

- Generate and print corresponding compiled Target code

- Run the compiled program

- Print out the return value

