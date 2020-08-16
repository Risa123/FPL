# FPL programming language
Name of this language is FPL(Function processing Language). Reason for this name is, than is was original meant as Lisp dialect. So the syntax can be diveded to lists and atoms. One of problems of lisp is a large amount of parathesises. This language has two types of lists. Block lists and statement lists. The block list is written inside of curly brackets. Statement lit is by new line.
Example:
{ <statement list>}
{
 <statement list>
 <statement list>
}
Compiler requires project directory as argument.  
Project directory must contain directory src for source files and file build.properties for build configuration.  
Format of build.properties is.  
outputFile = (output binary)  
cc = (C compiler command)  
mainModule = (main module)  
**This program requires java 14 preview or higher.**