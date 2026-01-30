@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar "%~dp0yacy-mcp.jar"
