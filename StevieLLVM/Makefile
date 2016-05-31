.phony:	all clean
.SUFFIXES:	.stv .s .jj .java

all:
	javacc Parser.jj
	javac  Src.java Parser.java llvm/*.java

clean:
	-rm ParseException.java Parser.java ParserConstants.java
	-rm ParserTokenManager.java SimpleCharStream.java
	-rm Token.java TokenMgrError.java
	-rm *.class demo demo.s demo.ll
	-rm llvm/*.class

.stv:
	java StevieLLVM < $<
	llc-3.4 -O2 -filetype=asm -march=x86-64 demo.ll
	clang -o demo demo.s runtime.c
