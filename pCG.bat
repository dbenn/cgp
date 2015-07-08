@echo off
rem DOS batch file to invoke Conceptual Graph Processes interpreter.

rem pCG root directory. Change "c:\cgp\" to your installation directory.

java -classpath c:\cgp\lib\antlr.jar;c:\cgp\lib\Notio.jar;c:\cgp\lib\cgp.jar;c:\cgp cgp.CGP %1 %2 %3 %4 %5 %6 %7 %8 %9
