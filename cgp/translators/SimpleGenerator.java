package cgp.translators;

import java.io.*;
import notio.*;
import notio.translators.DefiningLabelTable;

    /** 
     * An abstract base class for generators.
     *
     * @author Finnegan Southey
     * @version $Name:  $ $Revision: 1.2 $, $Date: 2001/06/28 11:57:16 $
		 * @legal Copyright (c) Finnegan Southey, 1996-1999
		 *	This program is free software; you can redistribute it and/or modify it 
		 *	under the terms of the GNU Library General Public License as published 
		 *	by the Free Software Foundation; either version 2 of the License, or 
		 *	(at your option) any later version.  This program is distributed in the 
		 *	hope that it will be useful, but WITHOUT ANY WARRANTY; without even the 
		 *	implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
		 *	See the GNU Library General Public License for more details.  You should 
		 *	have received a copy of the GNU Library General Public License along 
		 *	with this program; if not, write to the Free Software Foundation, Inc., 
		 *	675 Mass Ave, Cambridge, MA 02139, USA.
		 *
		 * Note that this is entirely unchanged, except for the
		 * package, since that must be the same as for CGIFGenerator.
		 * [dbenn]
		 *
     * @bug Escape sequences need work.
     */
abstract class SimpleGenerator implements Generator
  {
		/** Name of DefiningLabelTable unit in context. **/
	private static final String DEFINING_LABEL_TABLE_NAME = "DEFINING_LABEL_TABLE";
	
	  /** The writer to which the writer will write. **/
  Writer writer;
  
	  /** A knowledge base. **/
  KnowledgeBase knowledgeBase;
  
  	/** A translation context. **/
  TranslationContext translationContext;
  
  	/** The marker set from the knowledge base. **/
  MarkerSet markerSet;
  
  	/** The concept type hierarchy from the knowledge base. **/
  ConceptTypeHierarchy conceptHierarchy;
  
  	/** The relation type hierarchy from the knowledge base. **/
  RelationTypeHierarchy relationHierarchy;

    /**
     * Initializes the generator to write to the specified writer
     * using the specified TranslationContext and KnowledgeBase.
     *
     * @param newWriter  the writer to be generated to.
     * @param newKnowledgeBase the knowledge base to be used while generating.
     * @param newTranslationContext  the translationContext to be used while
     * generating.
     */
  public void initializeGenerator(Writer newWriter,
    KnowledgeBase newKnowledgeBase, 
    TranslationContext newTranslationContext) throws GeneratorException
    {
    writer = newWriter;
    initializeGenerator(newKnowledgeBase, newTranslationContext);
    }
    
    /**
     * Private initialization common to the public initializers.
     *
     * @param newKnowledgeBase the knowledge base to be used while generating.
     * @param newTranslationContext  the translationContext to be used while
     * generating.
     */
  private void initializeGenerator(KnowledgeBase newKnowledgeBase, 
    TranslationContext newTranslationContext) throws GeneratorException
    {
    knowledgeBase = newKnowledgeBase;
    translationContext = newTranslationContext;
    markerSet = knowledgeBase.getMarkerSet();
    conceptHierarchy = knowledgeBase.getConceptTypeHierarchy();
    relationHierarchy = knowledgeBase.getRelationTypeHierarchy();
    }

		/**
		 * Returns the DefiningLabelTable currently in used by this parser.
		 *
		 * @param translationContext  the translation context from which to get the table.
		 * @return the DefiningLabelTable currently in used by this parser.
		 */
	final DefiningLabelTable getDefiningLabelTable(TranslationContext translationContext)
		{
		DefiningLabelTable table;

		table = (DefiningLabelTable)translationContext.getUnit(DEFINING_LABEL_TABLE_NAME);

		if (table == null)
			{
			table = new DefiningLabelTable();
			table.setUnitName(DEFINING_LABEL_TABLE_NAME);
			translationContext.addUnit(table);
			}

		return table;
		}

		/**
		 * Appends the specified string to the StringBuffer or OutputStream
		 * with which the generator was initialized.
		 *
		 * @param txt  the string to be written.
		 */
	void generate(String txt) throws GeneratorException
		{
		try
			{		        
			writer.write(txt);
			}
		catch (IOException e)
			{
			throw new GeneratorException("Error encountered generating to writer.", e);
			}
		}

    /**
     * Returns a Class object that indicates what class the Unit parse
     * method will return.
     * 
     * @return a Class object that indicates what class the Unit parse
     * method will return.
     * @see notio.Generator#generateUnit
     */
  public Class getUnitClass()
  	{
  	try
  		{
	  	return Class.forName("notio.Graph");
	  	}
	  catch (ClassNotFoundException e)
	  	{
	  	e.printStackTrace();
	  	System.exit(1);
	  	}
	  	
  	return null;
  	}  

	/* Unit Rules */

    /**
     * Generates a graph to the output stream.
     *
     * @param unit  the unit object to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  public void generateUnit(Object unit) throws GeneratorException
    {
    generateGraph((Graph)unit);
    }

	/* Support Functions */
	
    /**
     * Adds escape sequences for specified characters whenever they occur
     * within the specified string.
     *
     * @param in  the string to be modified.
     * @param chars  a string containing all characters that need to be
     * escaped.
     * @param escapeSequence  the character that should prefix characters that
     * need escaping (e.g. backslash).
     * @return the modified string which now includes escape sequences.
     * @bug Do we need to do some more work for stuff like newlines and tabs?
     */
  public static String escapeCharactersInString(String in, String chars, 
    char escapeSequence)
    {
    int inlen;
    char curr;
    StringBuffer inbuff, outbuff;
    String esc;
    
    inlen = in.length();
    inbuff = new StringBuffer(in);
    outbuff = new StringBuffer(inlen * 2);
    esc = "" + escapeSequence;

    for (int chr = 0; chr < inlen; chr++)
      {
      curr = inbuff.charAt(chr);
      if (chars.indexOf(curr) != -1)
        outbuff.append(esc + curr);
      else if (curr == escapeSequence)
        outbuff.append(esc + curr);
      else
      	switch (curr)
      		{
      		case '\n':
      			outbuff.append(esc + "n");
      			break;
      			
      		case '\t':
      			outbuff.append(esc + "t");
      			break;
      			
      		case '\b':
      			outbuff.append(esc + "b");
      			break;
      			
      		case '\r':
      			outbuff.append(esc + "r");
      			break;
      			
      		case '\f':
      			outbuff.append(esc + "f");
      			break;      			
      			
      		default:
		        outbuff.append(curr);
      		}
      }
        
    return outbuff.toString();
    }
  }
