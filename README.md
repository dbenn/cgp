# cgp
Conceptual Graph processes

Preface (2015)
-------
This was an outcome of Masters work ending in 2001 as evidenced by the odd
anacronism. Small edits have been made but the text is largely as it was 
in 2001.

Introduction
------------
Welcome to the ICCS 2001 release of pCG, version 0.1!

The name "pCG" is symbolic of a process operating upon a CG.

Go to http://www.users.on.net/~dbenn/Masters/iccs2001/ for the context of this
language which is the outcome of my coursework Masters thesis at the
University of South Australia under the supervision of Dan Corbett.

Please send all comments and questions to me at dbenn@computer.org

Installation
------------
By reading this file, you've completed the most important step: unzipping the
archive.

The distribution comes with pre-built Java code (in the lib directory) and
invocation scripts for Unix and Windows: pCG and pCG.bat respectively. Change
the paths in the invocation script to match the place where you unzipped pCG
to yield a cgp directory.

Optionally add the script to your path. On a Win95/98 machine, you can do
this by adding to your c:\autoexec.bat file's exiting SET PATH command, or
adding after this a command such as:

        SET PATH=%PATH%;C:\cgp\pCG

assuming you have unzipped the archive at the top-level of your C drive. On
a Windows NT machine you or your administrator will have to add to the path
via the System Control Panel.

Under Unix, the path setting procedure will vary depending upon the shell
you're using. For csh and tcsh, add to the path line in your .cshrc file,
while for bash (e.g. under Linux) add to the PATH line in a manner such as
this:

        PATH=$PATH:$HOME/bin:/usr/local/Acrobat4/bin:$HOME/cgp

For Windows machines, reboot so that the new path comes into effect. For
Unix systems, source the .cshrc file or .bash_profile file (with the source
and "." commands respectively).

Moreover, if you have more than one JDK/JRE installed on your system or the
java executable is not on your path, you may wish to fully qualify the path
to the java interpreter in the pCG Unix shell script or DOS batch file, e.g.
in the pCG script, instead of:

        java -classpath $ANTLR_HOME/antlr.jar:...

you could have:

        /usr/local/jdk1.2.2/bin/java -classpath $ANTLR_HOME/antlr.jar:...

pCG *requires* Java 2, JDK/JRE 1.2.2. I make no guarantees about whether
it will work with any other version of Java at this time.

pCG comes with the Java class libraries it requires in cgp/lib, specifically
those for Antlr (2.7.0) and Notio (0.2.2).

pCG has been tested under Linux 2.2 (RedHat 6.0), Windows 98, and a few
versions of Solaris/SunOS, but primarily the first two, and more recently under Mac OS X. Despite being Java software, I won't make any claims about whether 
pCG will run on other operating systems, but please let me know if you are 
using another OS apart from the ones listed.

Usage
-----
pCG is a command-line interpreter but has no interactive mode. Edit a pCG
file, say "foo.cgp", with your favourite text editor, make sure pCG (or
pCG.bat if you're using Windows) is on your path or fully qualify its path,
and run it:

        pCG foo.cgp

Alternatively, you can add an interpreter invocation line to the top of
the file (given a Unix-style shell), e.g. see cgp/examples/gfx/tree.cgp.

Harry Delugach's excellent CharGer CG editor is useful for viewing files in
the examples directory with a ".cgf" or ".CGF" suffix.

To test pCG against the final CGTools workshop graph set, do the following
from a shell or MS-DOS prompt on your system (using the appropriate path
separator of course) where <root> is where you installed pCG:

 * Change directory to <root>/cgp/examples/CGTools01

 * Run pCG with final.cgp as the sole argument.

 * Read that program's comments for more information.

 * A minimal pCG program is also present in that directory, tiny.cgp,
   which opens a single file, reads a CG stream into a list, and prints
   each graph in it. Assuming pCG is on your path and the above directory
   is your working directory, you could type the following:

        pCG tiny.cgp final/final_graphs_level1.cgf

 * See the other program in that directory which relates to the basic
   graph set: basic-info.cgp

 * Programs for a simple actor and Mineau's iterative factorial process
   have also been converted to make use of the new June 2001 compliant
   CG Standard CGIF parser.

Note that pCG program files have by convention, a ".cgp" suffix, while CGIF
files end with ".cgf".

Note also that example programs have MS-DOS line termination character
sequences so that they may be viewed under both Unix and Windows.

pCG allows you to specify whether to generate CGIF or LF output, as well as
the kind of CGIF parser and CGIF generator. Look for "option cgifparser = ..."
and "option cgifgen = ..." in example pCG programs, which specify the Java
class to be used.

All graphs in code found in cgp/examples/CGTools01 are June 2001 CG Standard
compliant with respect to CGIF. Example code elsewhere still uses the older
CGIF version.

Also, some programs generate CGIF, LF, or a mixture, as output.

Documentation
-------------
The file pCG.pdf is based upon a chapter from my thesis which describes
pCG in some detail. References to other parts of the thesis should be
ignored, or you can download the thesis from the aforementioned URL.
Between this and the example programs, you should gain a good understanding
of the language. I may add more information later. Please ignore any message
you may see from Acrobat Reader regarding being unable to find the StarMath
font (which comes with Sun's StarOffice). I will fix this at some stage, but
there appears to be no impact with respect to reading the document.

The docs directory contains EBNF for pCG derived automatically from the ANTLR
grammars (docs/CGPParser.html, AntlrCGIFParser.html). JavaDocs generated from
the source code are also included (docs/index.html) and are definitive with
respect to pCG's run-time type attributes and operations. See the source code
(under cgp directory) for more non-JavaDoc comments.

Distribution
------------
pCG distribution is freely redistributable under the GPL version 2 licence.

Bugs
----
See the aforementioned web page for the current bug list. This software
requires real testing by real users, i.e. people other than me who are
going to try to apply it to their own problems.

If you're using pCG under Windows, you may see one or more errors such as
"non-fatal JIT error" when running pCG programs. You can ignore this as
there is no side effect that I've ever noticed. I have seen this error
even from commercial Java programs.

Build Notes
-----------
* Only people who wish to extend or modify pCG need read this section.

* The Makefile has only been tested under RedHat Linux 6.0, but should
  work under all Unices, and may be adaptable to other platforms with
  the make utility.

* If the make process stops with an error re: AntlrCGIFParser, just type
  make again and all will be well. There must be a bug in my Makefile
  but I haven't had time to track this down.

* The symbolic links to cgp/*TokenTypes.txt is necessary for the
  CGPInterpreter class's importVocab directive which seems to look in
  the Makefile's directory rather than in the cgp directory for
  the tokens file. This looks like a bug in ANTLR since exportVocab
  writes to the cgp directory. Need to workaound this since not all
  OSes have symbolic links which would mean taking a copy of the
  tokens file. How to fix? => i) move Makefile to cgp directory?;
  ii) run make from cgp/cgp directory?

*  Note that it should be possible to use $ANTLR_HOME/antlr.jar but a class
   required by SemanticException.toString() -- FileLineFormatter.class -- is
   not contained within antlr.jar, but is contained in $ANTLR_HOME/antlr.
   We get around this by calling SemanticException.getMessage().

* Please note that the Revision Control System (RCS) has been used for
  version control, hence the RCS directories everywhere.

Thanks
------
Terrence Parr's fantastic LL(k) parser generator tool, ANTLR, reduced the
size of the code of pCG by several thousand lines, and made it easy for me
to enforce a clean separation between the parser and the core interpreter.

Finnegan Southey's Notio Java API which embodies a significant part of the
functionality laid out in the proposed CG ANSI Standard, helped me enormously
in the implementation of pCG, particularly its CGIF parsing capability. In
order to make this conform to the June 2001 CG Standard however, I wrote my
own CGIF Parser, again, in ANTLR (see cgp/cgp/translators). Hopefully this
can be added to the Notio distribution. This is specified in pCG programs via
the directive: 'option cgifparser = "cgp.translators.CGIFParser";'.

Harry Delugach's CharGer CG editor has been a great help with actors and
general non-trivial CG creation and visualisation.

Guy Mineau, of course, for presenting the process formalism at ICCS 1998.

Dan Corbett, for his enthusisasm and encouragement re: my thesis work.

My wife Karen, for all the coffee and hugs.

David Benn, Adelaide, July 2001
