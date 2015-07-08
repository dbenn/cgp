/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2001 David Benn
 *
 * CGIF parser implementation class.
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
 * David Benn, June 2001.
 */

package cgp.translators;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Reader;

import antlr.CommonAST;
import antlr.RecognitionException;
import antlr.SemanticException;
import antlr.TokenStreamException;

import cgp.translators.AntlrCGIFLexer;
import cgp.translators.AntlrCGIFParser;
import cgp.translators.Parser;

import notio.Actor;
import notio.Concept;
import notio.Graph;
import notio.KnowledgeBase;
import notio.ParserException;
import notio.Relation;
import notio.TranslationContext;
import notio.UnimplementedFeatureException;

public class CGIFParser implements Parser {
    // Static fields.
    static private boolean treeOn = true; // for diagnostics

    // Instance fields.
    private String path = "<none>"; // e.g. source may be a string
    private DataInputStream stream;
    private AntlrCGIFLexer lexer;
    private AntlrCGIFParser parser;

    /**
     * Initializes the parser to parse the specified character reader.
     *
     * Precondition: File represents a readable CGIF file.
     *
     * @param f  the file whose contents are to be parsed.
     * @param kBase  the knowledge base to be used while parsing.
     * @param tContext  the translation context to be used while parsing.
     *
     * @exception ParserException  if an error occurs while initializing the 
     * parser.
     */
    public void initializeParser(File f,
				 KnowledgeBase kBase,
				 TranslationContext tContext /* unused */)
	throws ParserException {

	path = f.getPath();

	try {
	    stream = new DataInputStream(new FileInputStream(f));
	} catch (FileNotFoundException e) {
	    String msg = "Parser initialisation: ";
	    throw new ParserException(createExMsg(msg, e));
	}

	lexer = new AntlrCGIFLexer(stream);
	lexer.setFilename(path);
	
	parser = new AntlrCGIFParser(lexer, kBase);
	parser.setFilename(path);
    }

    /**
     * Initializes the parser to parse the specified character reader.
     *
     * Precondition: The string represents a readable CGIF file.
     *
     * @param s  the string whose contents are to be parsed.
     * @param kBase  the knowledge base to be used while parsing.
     * @param tContext  the translation context to be used while parsing.
     *
     * @exception ParserException  if an error occurs while initializing the 
     * parser.
     */
    public void initializeParser(String s,
				 KnowledgeBase kBase,
				 TranslationContext tContext /* unused */)
	throws ParserException {

	stream = new DataInputStream(new ByteArrayInputStream(s.getBytes()));
	
	lexer = new AntlrCGIFLexer(stream);
	lexer.setFilename(path); // default
	
	parser = new AntlrCGIFParser(lexer, kBase);
	parser.setFilename(path); // default
    }

    /**
     * Initializes the parser to parse the specified character reader.
     * 
     * @param reader  the reader whose contents are to be parsed.
     * @param kBase  the knowledge base to be used while parsing.
     * @param tContext  the translation context to be used while parsing.
     * @exception notio.ParserException  if an error occurs while initializing
     * the parser.
     * @exception UnimplementedFeatureException  if this parser does support
     * a java.io.Reader
     */
    public void initializeParser(Reader reader,
				 KnowledgeBase kBase,
				 TranslationContext tContext)
	throws ParserException {
	String msg = "Initialisation with Reader not supported.";
	throw new UnimplementedFeatureException(msg);
    }
  	
    /**
     * Returns a Class object that indicates what class the parseUnit()
     * method will return.
     * 
     * @return a Class object that indicates what class the parseUnit()
     * method will return.
     * @see notio.Parser#parseUnit
     */
    public Class getUnitClass() {
	return (new Graph()).getClass(); // must be a nicer way to do this
    }

    /**
     * Attempts to parse the default unit from the input stream.
     * The default unit is whatever a particular parser is usually
     * intended to parse.
     * 
     * @return the unit object parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     */
    public Object parseUnit() throws ParserException {
	String msg = null;

	try {
	    return parser.cg();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }

    /**
     * Attempts to parse a graph which is treated as the outermost context
     * for purposes for scoping.  This method should be used instead of
     * parseGraph() when no translation information is to be used from
     * previous translation sessions and when the parser can safely assume
     * that the graph is "self-contained".
     * 
     * @return the graph parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     * @exception notio.UnimplementedFeatureException  if this parser does
     * not support this parsing method.
     */
    public Graph parseOutermostContext() throws ParserException {
	String msg = "Outermost context parsing not supported.";
	throw new UnimplementedFeatureException(msg);
    }

    /**
     * Attempts to parse a graph stream from the input.
     *
     * @returns the graph stream parsed from the input.
     * @exception ParserException  if an error occurs while parsing.
     * @exception UnimplementedFeatureException  if this parser does not
     * support this parsing method.
     */
    public Graph[] parseGraphStream() throws ParserException {
	String msg = null;
	
	try {
	    return parser.cgStream();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }

    /**
     * Attempts to parse a graph from the input stream.
     * 
     * @return the graph parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     * @exception notio.UnimplementedFeatureException  if this parser does
     * not support this parsing method.
     */
    public Graph parseGraph() throws ParserException {
	String msg = null;

	try {
	    return parser.cg();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }

    /**
     * Attempts to parse a concept from the input stream.
     * 
     * @return the concept parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     * @exception notio.UnimplementedFeatureException  if this parser does
     * not support this parsing method.
     */
    public Concept parseConcept() throws ParserException {
	String msg = null;

	try {
	    return parser.concept();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }

    /**
     * Attempts to parse a relation from the input stream.
     * 
     * @return the relation parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     * @exception notio.UnimplementedFeatureException  if this parser does
     * not support this parsing method.
     */
    public Relation parseRelation() throws ParserException {
	String msg = null;

	try {
	    return parser.relation();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }
  
    /**
     * Attempts to parse an actor from the input stream.
     * 
     * @return the actor parsed from the input stream.
     * @exception notio.ParserException  if an error occurs while parsing.
     * @exception notio.UnimplementedFeatureException  if this parser does
     * not support this parsing method.
     */
    public Actor parseActor() throws ParserException {
	String msg = null;

	try {
	    return parser.actor();
	}
	catch(SemanticException e) {
	    msg = "Semantic error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(RecognitionException e) {
	    msg = "Syntax error (recognition) [line " + e.getLine() + "]: ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(TokenStreamException e) {
	    msg = "Syntax error (token stream): ";
	    throw new ParserException(createExMsg(msg, e));
	}
	catch(Exception e) {
	    msg = "Error: ";
	    throw new ParserException(createExMsg(msg, e));
	}
    }

    /**
     * Returns a parse tree, if available, as a string. This may be empty.
     * This can be useful for diagnostic purposes.
     *
     * Pre-condition: a parser exists and a parsing method has been invoked.
     *
     * @returns  the parse tree from the last parsing operation.
     * @exception UnimplementedFeatureException  if this parser does not
     * support the returning of a parse tree.
     */
    public String getParseTree() {
	CommonAST tree = (CommonAST)parser.getAST();
	return (tree != null) ? tree.toStringList() : "empty";
    }

    /**
     * Creates a message for a parser exception given another exception.
     * The parse tree is also optionally included.
     *
     * @param prefix  some preamble text.
     * @param e  the exception
     * @return  the message string
     */
    private String createExMsg(String prefix, Exception e) {
	String msg = prefix + e.getMessage();
	if (treeOn) msg += "\n\nParse tree:\n" + getParseTree();
	return msg;
    }

    /**
     * Return the parser object.
     *
     * @return  the parser object
     */
    public AntlrCGIFParser getParser() {
	return parser;
    }
}
