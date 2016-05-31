This folder contains a version of the StevieFun compiler that generates
output programs in LLVM format.  It depends on some specific versions of
the LLVM tools (3.4) and the Java Development Kit (JDK 1.8) that are
installed on the LinuxLab machines, and will require some adjustments to
work with other versions of these tools.  (For example, some parts of the
LLVM assembly language syntax have changed between versions 3.4 and 3.8,
the former being the version that is installed on the LinuxLab, the latter
being the most recent version available from llvm.org at the time of
writing.)

In particular, the Java source for this version of the Stevie compiler
makes use of lambda expressions, which are a new Java feature that was
introduced in Java 8 (i.e., JDK 1.8).  As such, the code will not compile
using earlier versions of Java.  To check that you are set up to use this
version of Java, run the "addpkg" command on the LinuxLab machines and
make sure that java8 is selected.  I believe that you will need to log out
and log back in again for the new package selection to take effect.

The program may be compiled using the supplied Makefile by typing:

  make

(You can also use "make clean" to remove the compiled files.)

To use the resulting compiler, use a command of the following form:

  java StevieLLVM < sourcefile.stv

where sourcefile.stv contains the source code that you want to compile.
This will produce an llvm assembly code output in the file "demo.ll".

To generate a corresponding x86_64 assembly language file, "demo.s":

  llc-3.4 -O0 -filetype=asm -march=x86-64 demo.ll

And to turn this into an executable program, "demo", use:

  clang -o demo demo.s runtime.c

This program can now be executed using the command:

  ./demo

In fact, there is a rule in the Makefile that should allow you to automate
the above steps by typing:

  make sourcefile

to compile a Stevie program whose source is in sourcefile.stv.  (The
generated LLVM source file and the executable file will still be called
demo.ll and demo, respectively).

