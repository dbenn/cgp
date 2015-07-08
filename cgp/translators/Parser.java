/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2001 David Benn
 *
 * CGIF parser interface.
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

import java.io.File;
import notio.Graph;
import notio.KnowledgeBase;
import notio.ParserException;
import notio.TranslationContext;

/**
 * Additional functionality to support ANTLR CGIF Parser and June 2001
 * CG Standard which includes CG Streams. Also, ANTLR requires an
 * InputStream not a Reader, so this interface is generic enough to
 * take input from a file or string, letting the implementation deal
 * with the details of how to read from the supplied source.
 */
public interface Parser extends notio.Parser {
    /**
     * Initializes the parser to parse the specified character reader.
     *
     * Precondition: The file represents a readable CGIF file.
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
				 TranslationContext tContext)
	throws ParserException;

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
				 TranslationContext tContext)
	throws ParserException;

    /**
     * Attempts to parse a graph stream from the input.
     *
     * @returns  the graph stream parsed from the input as an array of graphs.
     * @exception ParserException  if an error occurs while parsing.
     * @exception UnimplementedFeatureException  if this parser does not
     * support this parsing method.
     */
    public Graph[] parseGraphStream() throws ParserException;

    /**
     * Returns a parse tree, if available, as a string. This may be empty.
     * This can be useful for diagnostic purposes.
     *
     * Pre-condition: a parser exists and a parsing method has been invoked.
     *
     * @returns  the parse tree
     * @exception UnimplementedFeatureException  if this parser does not
     * support the returning of a parse tree.
     */
    public String getParseTree();
}
