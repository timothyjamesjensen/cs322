.phony:	all clean

all:
	javacc Parser.jj
	javac  Src.java Parser.java Target.java

clean:
	-rm ParseException.java Parser.java ParserConstants.java
	-rm ParserTokenManager.java SimpleCharStream.java
	-rm Token.java TokenMgrError.java
	-rm *.class

