all: BasicFTP.jar
BasicFTP.jar: BasicFTP.java
	javac BasicFTP.java
	jar cvfe BasicFTP.jar BasicFTP *.class


run: BasicFTP.jar  
	java -jar BasicFTP.jar ftp.cs.ubc.ca  21

clean:
	rm -f *.class
	rm -f BasicFTP.jar
