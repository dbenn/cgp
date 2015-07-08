/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2000,2001 David Benn
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Conceptual Graph Processes interpreter invocation code.
 *
 * David Benn, June-November 2000, June 2001.
 */

package cgp;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import antlr.CommonAST;
import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.TokenStreamException;
import cgp.CGPLexer;
import cgp.CGPParser;
import cgp.CGPInterpreter;
import cgp.runtime.LastException;
import notio.KnowledgeBase;
import notio.ParserException;
import notio.TranslationContext;

public class CGP {
    // Static fields.
    static private int ERR = -1; 
    static private boolean traceOn;
   
    // Options.
    static public boolean LFOpt = false;
    static public String CGIFParserOpt = "notio.translators.CGIFParser";
    static public String CGIFGenOpt = "notio.translators.CGIFGenerator";

    // Static methods.
    static public void setTraceMode(boolean state) {
	traceOn = state;
    }

    public static boolean isTraceOn() {
	return traceOn;
    }

    // The next two methods create graph parser objects given a 
    // file or string. This makes it possible to load a new parser
    // at run-time. Also, Notio parsers use Reader objects, while
    // others may use InputStreams. These factory methods hide such
    // details from the caller, presenting the latter with a generic
    // Notio Parser interface. Note that cgp.translators.Parser is a
    // subinterface of notio.Parser

    public static notio.Parser createCGParser(String parserClass,
					      File f,
					      KnowledgeBase kb,
					      TranslationContext tc)
	throws ParserException {
	try {
	    if (parserClass.startsWith("notio")) {
		notio.Parser p = (notio.Parser)
		    (Class.forName(parserClass)).newInstance();
		p.initializeParser(new FileReader(f), kb, tc);
		return p;
	    } else if (parserClass.startsWith("cgp")) {
		cgp.translators.Parser p = (cgp.translators.Parser)
		    (Class.forName(parserClass)).newInstance();
		p.initializeParser(f, kb, tc);
		return p;
	    } else {
		throw new ParserException(parserClass +
					  ": unknown graph parser.");
	    }
	} catch (Exception e) {
	    throw new ParserException("Error creating " + parserClass +
				      " (" + e.getMessage() + ")");
	}
    }

    public static notio.Parser createCGParser(String parserClass,
					      String s,
					      KnowledgeBase kb,
					      TranslationContext tc)
	throws ParserException {
	try {
	    if (parserClass.startsWith("notio")) {
		notio.Parser p = (notio.Parser)
		    (Class.forName(parserClass)).newInstance();
		p.initializeParser(new StringReader(s), kb, tc);
		return p;
	    } else if (parserClass.startsWith("cgp")) {
		cgp.translators.Parser p = (cgp.translators.Parser)
		    (Class.forName(parserClass)).newInstance();
		p.initializeParser(s, kb, tc);
		return p;
	    } else {
		throw new ParserException(parserClass +
					  ": unknown graph parser.");
	    }
	} catch (Exception e) {
	    throw new ParserException(e.getMessage());
	}	
    }

    public static void main(String[] args) {
	try {
	    DataInputStream dis = null;
	    String fileName = null;
	    try {
		if (args.length >= 1) {
		    fileName = args[0];
		    File f = new File(args[0]);
		    dis = new DataInputStream(new FileInputStream(f));
		} else {
		    System.err.println("No pCG file specified.");
		    System.exit(ERR);
		}
	    } catch(FileNotFoundException e) {
		System.err.println("File error: " + e.getMessage());
		System.exit(ERR);
	    }

	    CGPLexer lexer = new CGPLexer(dis);
	    lexer.setFilename(fileName);

	    // Parse and build a parse tree.
	    CGPParser parser = new CGPParser(lexer);
	    parser.setFilename(fileName);
	    parser.program();

	    // Obtain the parse tree.
	    CommonAST tree = (CommonAST)parser.getAST();

	    // Walk the tree, interpreting the code.
	    CGPInterpreter interpreter = new CGPInterpreter(args, tree);
	    interpreter.program(tree);
	}
	catch(SemanticException e) {
	    if (!traceOn) {
		System.err.println("Semantic error: " + e.getMessage());
	    } else {
		System.out.print("TRACE: ");
		e.printStackTrace();
	    }
	}
	catch(RecognitionException e) {
	    if (!traceOn) {
		System.err.println("Syntax error (recognition): " +
				   e.getLine() + ": " + e.getMessage());
	    } else {
		System.out.print("TRACE: ");
		e.printStackTrace();
	    }
	}
	catch(TokenStreamException e) {
	    if (!traceOn) {
		System.err.println("Syntax error (token stream): " + e);
	    } else {
		System.out.print("TRACE: ");
		e.printStackTrace();
	    }
	}
	catch (LastException e) {
	    if (!traceOn) {
		System.err.println("'last' statement not within a loop.");
	    } else {
		System.out.print("TRACE: ");
		e.printStackTrace();
	    }	    
	}
	catch(Exception e) {
	    if (!traceOn) {
		System.err.println("Error: " + e);
	    } else {
		System.out.print("TRACE: ");
		e.printStackTrace();
	    }
	}
    }
}
