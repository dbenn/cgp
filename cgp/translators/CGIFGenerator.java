package cgp.translators;

import cgp.translators.DefinedQuantifier;
import cgp.translators.SimpleGenerator;

import notio.*;
import notio.translators.DefiningLabelTable;

    /** 
     * A CGIF Generator class.
     *
     * @author Finnegan Southey
     * @version $Name:  $ $Revision: 1.3 $, $Date: 2001/06/28 12:44:23 $
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
     * @bug Should add 'useNegationAbbreviation' flag to generator.
     * @bug Have not implemented USE_LITERAL_CONCEPTS_IN_RELATORS.
     *
     * Note: This is a lightly hacked version of Finnegan's generator which
     *       makes the output compliant with the June 2001 CG Standard. Ah,
     *       the benefits of freely available source code. [dbenn]
     *       
     *
     */
public class CGIFGenerator extends SimpleGenerator implements Generator
  {
		/** Name of MarkerTable unit in context. **/
	private static final String MARKER_TABLE_NAME = "MARKER_TABLE";
	
		/**	Type label for negation	relation type. **/
	private	static final String	NEG_TYPE_LABEL = "Neg";

		/**	Flag that enables 'inlining' of concepts within relations where possible. **/
	private	static final boolean USE_LITERAL_CONCEPTS_IN_RELATORS = true;


		/** Flag indicating whether the generator should allow incomplete relations or
		 * throw an exception. **/
	private boolean allowIncompleteRelations = false;

		/** Flag indicating whether the generator should supress node comments. **/
	private boolean supressNodeComments;

		/** Flag indicating whether the generator should supress graph comments. **/
	private boolean supressGraphComments;

    /**
     * Initializes the generator to write to the specified writer
     * using the specified TranslationContext and KnowledgeBase.
     *
     * @param newWriter  the writer to be generated to.
     * @param newKnowledgeBase the knowledge base to be used while generating.
     * @param newTranslationContext  the translationContext to be used while
     * generating.
     */
  public void initializeGenerator(java.io.Writer newWriter,
    KnowledgeBase newKnowledgeBase, 
    TranslationContext newTranslationContext) throws GeneratorException
    {
    super.initializeGenerator(newWriter, newKnowledgeBase, newTranslationContext);
    }

		/** 
		 * Sets a flag indicating whether incomplete relations are allowed whilst generating.
		 * If this flag is false, a GeneratorException will be thrown when an incomplete relation
		 * is encountered.  If it is true, any incomplete arguments will simply be skipped
		 * as if they did not exist.  
		 * If incomplete relations are allowed there is no guarantee that any CGIF parser will
		 * accept the generate output if it enforces relation valence or type definitions.
		 * The default setting for this flag is false.
		 *
		 * @param flag  the new value for the flag.
		 *
		 * @see notio.Relation#isComplete
		 */
  public void setAllowIncompleteRelations(boolean flag)
  	{
  	allowIncompleteRelations = flag;
  	}
    
		/** 
		 * Returns a flag indicating whether incomplete relations are allowed whilst generating.
		 * If this flag is false, a GeneratorException will be thrown when an incomplete relation
		 * is encountered.  If it is true, any incomplete arguments will simply be skipped
		 * as if they did not exist.  
		 * If incomplete relations are allowed there is no guarantee that any CGIF parser will
		 * accept the generate output if it enforces relation valence or type definitions.
		 * The default setting for this flag is false.
		 *
		 * @return  the value for the flag.
		 *
		 * @see notio.Relation#isComplete
		 */
  public boolean getAllowIncompleteRelations()
  	{
  	return allowIncompleteRelations;
  	}
    
		/** 
		 * Sets a flag indicating whether this generator should supress comments in nodes
		 * (Concepts, Relations, and Actors).
		 * The default setting for this flag is false.
		 *
		 * @param flag  the new value for the flag.
		 */
  public void setSupressNodeComments(boolean flag)
  	{
  	supressNodeComments = flag;
  	}
    
		/** 
		 * Returns a flag indicating whether this generator will supress comments in nodes
		 * (Concepts, Relations, and Actors).
		 * The default setting for this flag is false.
		 *
		 * @return the value for the flag.
		 */
  public boolean getSupressNodeComments()
  	{
  	return supressNodeComments;
  	}
    
		/** 
		 * Sets a flag indicating whether this generator should supress comments in graphs.
		 * The default setting for this flag is false.
		 *
		 * @param flag  the new value for the flag.
		 */
  public void setSupressGraphComments(boolean flag)
  	{
  	supressGraphComments = flag;
  	}
    
		/** 
		 * Returns a flag indicating whether this generator will supress comments in graphs.
		 * The default setting for this flag is false.
		 *
		 * @return the value for the flag.
		 */
  public boolean getSupressGraphComments()
  	{
  	return supressGraphComments;
  	}
    
    
	/* Graph Rules */

    /**
     * Generates a graph to the output stream.
     *
     * @param graph  the graph to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     *
     * @bug Does not properly treat translation context.
     */
  public void generateOutermostContext(Graph graph) throws GeneratorException
    {
    Graph(graph, getDefiningLabelTable(translationContext));
		}

    /**
     * Generates a graph to the output stream.
     *
     * @param graph  the graph to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  public void generateGraph(Graph graph) throws GeneratorException
    {
    Graph(graph, getDefiningLabelTable(translationContext));
		}

    /**
     * Generates a concept to the output stream.
     *
     * @param concept  the concept to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  public void generateConcept(Concept concept) throws GeneratorException
    {
    Concept(concept, getDefiningLabelTable(translationContext));
    }
    
    /**
     * Generates a relation to the output stream.
     *
     * @param relation  the relation to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  public void generateRelation(Relation relation) throws GeneratorException
    {
    Relation(relation, getDefiningLabelTable(translationContext));
    }
    
    /**
     * Generates an actor to the output stream.
     *
     * @param relation  the relation to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  public void generateActor(Actor actor) throws GeneratorException
    {
    Actor(actor, getDefiningLabelTable(translationContext));
    }

	/* Real generation methods. */    
    
    /**
     * Generates a graph to the output stream.
     *
     * @param graph  the graph to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void Graph(Graph graph, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    Concept concepts[];
    Relation relations[];
    String comments[];
    int numConcepts, numRelations, numComments;

		// Push new context onto stack
		definingLabelTable.pushContext();

    // Generate concepts, defining concepts first, non-defining dominant next,
    // and subordinate last.
    concepts = graph.getConcepts();
    numConcepts = concepts.length;
   
    for (int con = 0; con < numConcepts; con++)
    	{
    	CoreferenceSet corefSets[];
    	
    	corefSets = concepts[con].getCoreferenceSets();
    	
    	for (int set = 0; set < corefSets.length; set++)
    		{
    		try
    			{
    			corefSets[set].setEnableScopeChecking(true);
    			}
    		catch (CorefAddException e)
    			{
    			throw new GeneratorException("Error enabling scope checking in concept.", e);
    			}
    		catch (InvalidDefiningConceptException e)
    			{
    			throw new GeneratorException("Error enabling scope checking in concept.", e);
    			}
    		}
    	}
    	
    // Generate defining concepts
    for (int con = 0; con < numConcepts; con++)
      if (concepts[con].isDefiningConcept())
        Concept(concepts[con], definingLabelTable);

		// Generate non-defining dominant concepts
    for (int con = 0; con < numConcepts; con++)
      if (!concepts[con].isDefiningConcept() && concepts[con].isDominantConcept())
        Concept(concepts[con], definingLabelTable);

		// Generate subordinate concepts
    for (int con = 0; con < numConcepts; con++)
      if (!concepts[con].isDominantConcept())
        Concept(concepts[con], definingLabelTable);

		// Generate relations and actors.
    relations = graph.getRelations();
    numRelations = relations.length;

    for (int rel = 0; rel < numRelations; rel++)
    	if (relations[rel] instanceof Actor)
	    	Actor((Actor)relations[rel], definingLabelTable);
	    else
	      Relation(relations[rel], definingLabelTable);
		
		// Generate graph comments if not supressed
		if (!supressGraphComments)
			{
			comments = graph.getComments();
			numComments = comments.length;

			for (int com = 0; com < numComments; com++)
				GraphComment(comments[com]);
			}
				
		// Pop context from stack
		definingLabelTable.popContext();
    }

    /**
     * Generates a graph comment to output stream.
     *
     * @param comment  the graph comment to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void GraphComment(String comment) throws GeneratorException
    {
    if (comment == null)
      return;

    if (comment.length() == 0)
      return;

    // The June 2001 CG Standard now uses ;...; not /*...*/
    generate(";" + comment + ";");
    }

	/* Concept Rules */
	
    /**
     * Generates a concept to the output stream.
     *
     * @param concept  the concept to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     *
     * @bug Perhaps we should assume a concept was parsed as a literal in a
     * relation if it has no coreference labels and not display it?  Sounds
     * dangerous.  Is there some way to do this intelligently so we don't risk
     * omitting it?  What if it's not a member of a relationship?
     * @bug Removed exception thrown when there is a reference to a coref set that is not
     * in the table by a concept that has multiple sets.  This whole coref thing needs to
     * be reconsidered in light of the idea that single concepts can be generated.
     * @bug Need to review colon generation conditions.
     */
  void Concept(Concept concept, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    // If a concept is already in the table as a defining concept, write it out with
    //   the corresponding defining label (it's probably a formal parameter).
    // If a concept has no relators or coref sets, just write it out without labels
    // If a concept has relators but no coref sets, create a defining label for it
    //	 as a defining concept and write it out.
    // If a concept has coref sets, but no relators, get the label for the coref set,
    //   or if none exists, create a label and add it for the coref set.  Write out as
    //   bound or defining as appropriate.
    // If a concept has coref sets and relators, treat it as above. ??    
    // Special case: If a concept has one relator and no coref sets, we could write it as
    // a literal once we reach the relator.
    ConceptType conType;
    Referent referent;
    String corefLabel;
    String corefStuff = "";
    CoreferenceSet corefSets[];

		// Grab some objects from the concept
    conType = concept.getType();
    referent = concept.getReferent();

		// Generate concept start
    generate("[");

    // Generate typefield
    if (conType != null)
    	{
    	ConceptType(conType);
    	}

    // Generate defining or bound labels (if any)
    // Operates under the assumption that if a coref set is in
    // the table, a defining concept has already been generated for it and so
    // we can make bound label references.

		// If the concept is in the table as a defining concept, then simply write out
		// with the appropriate label.  It was probably used as a formal parameter.
		corefLabel = definingLabelTable.getDefiningLabelByDefiningConcept(concept);

		if (corefLabel == null)
			{
    	corefSets = concept.getCoreferenceSets();

    	// if it has zero coref sets, but has one or more relators, create a defining 
    	// label for it and establish it as a defining concept in the table
    	if (corefSets == null || corefSets.length == 0)
      	{
      	Relation relators[];
	     
     		// Check to see if it has one or more relators before bothering to create a label 
				// for it
      	relators = concept.getRelators();
      	if ((relators != null) && (relators.length > 0))
      		{      
					corefLabel = definingLabelTable.getNextAvailableDefiningLabel();

					definingLabelTable.mapDefiningLabelToDefiningConcept(corefLabel, concept);
					corefStuff += "*" + corefLabel;
					}
 	    	} 	    
    	else if (corefSets.length == 1)      // if it has one coref set
      	{
      	corefLabel = definingLabelTable.getDefiningLabelByCoreferenceSet(corefSets[0]);
	      
      	// if coref set is in table, use label from table as bound label
      	if (corefLabel != null)
        	corefStuff += "?" + corefLabel;
      	else
        	{
        	// if label from coref set is not in table check in defining concept table
        	corefLabel = definingLabelTable.getDefiningLabelByDefiningConcept(concept);
	        
        	if (corefLabel != null && 
          	(definingLabelTable.getCoreferenceSetByDefiningLabel(corefLabel) == null))
          	{
          	// if corefSet has a defining concept label that is not currently in use, add 
						// mapping to table and use label
          	definingLabelTable.mapDefiningLabelToCoreferenceSet(corefLabel, corefSets[0]);
          	}
        	else
          	// Create a new label, add mapping to table, and use label
          	{
          	corefLabel = definingLabelTable.getNextAvailableDefiningLabel();
          	definingLabelTable.mapDefiningLabelToCoreferenceSet(corefLabel, corefSets[0]);
          	definingLabelTable.mapDefiningLabelToDefiningConcept(corefLabel, concept);
          	}
	          
        	corefStuff += "*" + corefLabel;
        	}
      	}
    	else
      	// else for each coref set
      	for (int setCount = 0; setCount <  corefSets.length; setCount++)
					{
					corefLabel = definingLabelTable.getDefiningLabelByCoreferenceSet(corefSets[setCount]);
					
        	// if coref set is in table, use label from table as bound label
        	if (corefLabel != null)
          	corefStuff +="?" + corefLabel;
        	else
        		{
	/*          // This shouldn't happen right?  It means we haven't seen the
          	// defining concept yet and that's bad.  We generate the
          	// defining concept before any others.  
	          
          	throw new GeneratorException("Bound reference to unlabelled coreference set.");
	          
	*/
						// Formerly, this threw an exception but we'll make it add a label for now until
						// we rethink the whole thing.
      	  	// Create a new label, add mapping to table, and use label
    	    	corefLabel = definingLabelTable.getNextAvailableDefiningLabel();
  	      	definingLabelTable.mapDefiningLabelToCoreferenceSet(corefLabel, corefSets[setCount]);
	        	corefStuff += "*" + corefLabel;
        		}
        	}
      }
    else
    	{
    	corefStuff += "*" + corefLabel;
    	}

		// Decide if colon should be generated
		if ((conType != null) && (!corefStuff.equals("") || (!concept.isGeneric())))
			generate(":");

    // Generate referent
    Referent(referent, definingLabelTable);

    // Generate coreference stuff.
    // In the June 2001 CG Standard, coref labels must come
    // before the colon or after the referent. [dbenn]
    generate(corefStuff);

    // Generate concept comment
 		if (!supressNodeComments)
			{
	    ConceptComment(concept.getComment());
	    }
    
    generate("]");
    }

    /**
     * Generates a concept type to the output stream.
     *
     * @param conType  the concept type to be generated.
     * @exception GeneratorException  if an IO error occurs.
     */
  void ConceptType(ConceptType conType) throws GeneratorException
  	{
  	ConceptTypeDefinition typeDef;
  	
    if (conType.getLabel() == null)
    	{
    	typeDef = conType.getTypeDefinition();
    	
    	if (typeDef == null)
    		{
    		// No label and no type definition found.  I guess we can consider it an
    		// 'anonymous and undefined' type and simple leave the field blank.
    		return;
    		}
    	else
    		{
    		// Generate the type definition.
    		ConceptTypeDefinition(typeDef);
    		}
    	}
    else
    	generate(conType.getLabel());
    }

    /**
     * Generates a concept type definition to the output stream.
     *
     * @param typeDef  the concept type definition to be generated.
     * @exception GeneratorException  if an IO error occurs.
     */
  void ConceptTypeDefinition(ConceptTypeDefinition typeDef) throws GeneratorException
  	{
  	Graph differentia;
  	Concept formalParam;
  	ConceptType signature;
  	String corefLabel;
  	DefiningLabelTable definingLabelTable = new DefiningLabelTable();
  	
  	generate("(lambda(");
  	
  	// Generate signature
		formalParam = typeDef.getFormalParameter();
		if (formalParam == null)
			{
			signature = typeDef.getSignature();
			if (signature == null)
				throw new GeneratorException("No formal parameters or signature found in type definition.");
			else
				{
				ConceptType(signature);
				corefLabel = definingLabelTable.getNextAvailableDefiningLabel();
				generate("*" + corefLabel);
				}
			}
		else
			{
			signature = formalParam.getType();
			
			// Untyped formal parameters are assumed to use the Universal type.
			if (signature == null)
				signature = knowledgeBase.getConceptTypeHierarchy().getTypeByLabel(ConceptTypeHierarchy.UNIVERSAL_TYPE_LABEL);
				
			ConceptType(signature);
			
			// Output defining label for formal parameter.
			corefLabel = definingLabelTable.getNextAvailableDefiningLabel();

			definingLabelTable.mapDefiningLabelToDefiningConcept(corefLabel, formalParam);
			generate("*" + corefLabel);
			}
			
  	generate(")");
  	
  	// Generate differentia (if any).
  	differentia = typeDef.getDifferentia();
  	if (differentia != null)
  		{
  		// Should probably do some context stuff here to ensure the labels are fresh.
  		Graph(differentia, definingLabelTable);
  		}
  		
  	generate(")");  	
  	}
  	
    /**
     * Generates a referent to the output stream.
     *
     * @param referent  the referent to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an IO error occurs.
     */
  void Referent(Referent referent, DefiningLabelTable definingLabelTable) throws GeneratorException
  	{
  	Graph descriptor;
  	
  	if (referent == null)
  		return;
  	
 		Quantifier(referent.getQuantifier());
		Designator(referent.getDesignator(), definingLabelTable);
		
		descriptor = referent.getDescriptor();
		if (descriptor != null)
			Graph(descriptor, definingLabelTable);
  	}
  	
    /**
     * Generates a quantifier to the output stream.
     *
     * This now uses pCG's quantifier classes. [dbenn]
     *
     * @param quantifier  the quantifier to be generated.
     * @exception GeneratorException  if an IO error occurs.
     */
   void Quantifier(Macro quantifier) throws GeneratorException
    {
	if (quantifier != null) {
	    String quant = "";
	    // For example: @3, @every
	    quant = "@" + quantifier.getName();
	    if (quantifier instanceof DefinedQuantifier) {
		// Collection or no?
		Object[] result = quantifier.executeMacro(null);
		if (result != null) {
		    // For example: @Col{"red", "green", "blue"}
		    quant += "{";
		    for (int i=0;i<result.length;i++) {
			quant += "\"" + (String)result[i] + "\"";
			if (i != result.length-1) quant += ", ";
		    }
		    quant += "}";
		}
	    }
	    
	    generate(quant);
	}
    }

    /**
     * Generates a designator to the output stream.
     *
     * @param designator  the designator to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void Designator(Designator designator, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    if (designator == null)
      return;

    switch (designator.getDesignatorKind())
      {
      case Designator.DESIGNATOR_LITERAL:
      	{
				LiteralDesignator((LiteralDesignator)designator);
        break;
        }

      case Designator.DESIGNATOR_MARKER:
        {
				MarkerDesignator((MarkerDesignator)designator);
        break;
        }

      case Designator.DESIGNATOR_NAME:
        {
				NameDesignator((NameDesignator)designator);
        break;
        }

      case Designator.DESIGNATOR_DEFINED:
       	throw new GeneratorException("Defined designators not handled by this generator.");
        
      default:
       	throw new GeneratorException("Unknown designator kind encountered in concept.");
       	
      }
    }

    /**
     * Generates a literal designator to output stream.
     *
     * @param designator  the designator to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void LiteralDesignator(LiteralDesignator designator) throws GeneratorException
  	{
  	Object literal;
      	
   	literal = designator.getLiteral();
   	if (literal instanceof String)
       generate("\"" + escapeCharactersInString(literal.toString(), "\"", '\\') + "\"");
	else if (literal instanceof Number) {
	    // The June 2001 CG Standard requires "+" before positive 
	    // numbers. [dbenn]
	    if (((Number)literal).doubleValue() >= 0) generate("+"); 
	    generate(literal.toString()); // also, removed a leading space 
	} else
      throw new GeneratorException("Unknown type of literal designator found in concept.");
  	}
    
    /**
     * Generates a marker designator to output stream.
     *
     * @param designator  the designator to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void MarkerDesignator(MarkerDesignator designator) throws GeneratorException
    {
    Marker marker;
    String markerID;

    marker = designator.getMarker();
    generate("#" + marker.getMarkerID());
    }
    
    /**
     * Generates a name designator to output stream.
     *
     * @param designator  the designator to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void NameDesignator(NameDesignator designator) throws GeneratorException
    {
    String name;

    name = designator.getName();
    generate("'" + escapeCharactersInString(name, "'", '\\') + "'");
    }
    
    /**
     * Generates a concept comment to output stream.
     *
     * @param comment  the concept comment to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void ConceptComment(String comment) throws GeneratorException
    {
    if (comment == null)
      return;

    if (comment.length() == 0)
      return;

    generate(";"+escapeCharactersInString(comment, "]", '\\'));
    }

	/* Relation Rules */

    /**
     * Generates a relation to the output stream.
     *
     * @param relation  the relation to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     *
     * @bug Literal concept handling is just plain wrong.
     * @bug Removed throwing of exceptions when there is no coref label since
     * just doesn't make sense if we want to print a single relation.
     * @bug Can relations not have a blank type field?
     */
  void Relation(Relation relation, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    RelationType relType;
    Concept arguments[];

		// Generate relation start
    generate("(");
   
    // Generate typefield
    relType = relation.getType();
    if (relType != null)
    	RelationType(relType);
    else
      throw new GeneratorException("No type found while generating relation.");

    // Generate arguments as bound labels
    arguments = relation.getArguments();

    for (int arg = 0; arg < arguments.length; arg++)
      Arc(arguments[arg], definingLabelTable);

		// Generate relation comment
 		if (!supressNodeComments)
	    RelationComment(relation.getComment());

		// Generate relation terminator
    generate(")");
    }

    /**
     * Generates a relation type to the output stream.
     *
     * @param relType  the relation type to be generated.
     * @exception GeneratorException  if an IO error occurs.
     *
     * @bug What about types with no label or definition?  Anonymous types?
     * @bug What about types with both a label and a definition?
     */
  void RelationType(RelationType relType) throws GeneratorException
  	{
  	RelationTypeDefinition typeDef;
    if (relType.getLabel() == null)
    	{
    	// Generate type definition here instead as a lambda expression
    	typeDef = relType.getTypeDefinition();
    	
    	if (typeDef != null)
    		RelationTypeDefinition(typeDef);
    	else
    		throw new GeneratorException("Encountered relation type with no label or definition.");
    	}
    else
    	generate(relType.getLabel());
    }

    /**
     * Generates a relation type definition to the output stream.
     *
     * @param typeDef  the relation type definition to be generated.
     * @exception GeneratorException  if an IO error occurs.
     */
  void RelationTypeDefinition(RelationTypeDefinition typeDef) throws GeneratorException
  	{
  	Graph differentia;
  	Concept formalParams[];
  	ConceptType signature[];
  	String corefLabel;
  	DefiningLabelTable definingLabelTable = new DefiningLabelTable();
  	
  	generate("(lambda(");
  	
  	// Generate signature
		formalParams = typeDef.getFormalParameters();
		if (formalParams == null)
			{
			signature = typeDef.getSignature();
			if (signature == null)
				throw new GeneratorException("No formal parameters or signature found in type definition.");
			else
				{
				for (int sig = 0; sig < signature.length; sig++)
					{
					if (sig > 0)
						generate(",");
						
					ConceptType(signature[sig]);
					corefLabel = definingLabelTable.getNextAvailableDefiningLabel();
					generate("*" + corefLabel);
					}
				}
			}
		else
			{
			for (int sig = 0; sig < formalParams.length; sig++)
				{
				ConceptType sigType;
				
				if (sig > 0)
					generate(",");

				sigType = formalParams[sig].getType();
			
				// Untyped formal parameters are assumed to use the Universal type.
				if (sigType == null)
					sigType = knowledgeBase.getConceptTypeHierarchy().getTypeByLabel(ConceptTypeHierarchy.UNIVERSAL_TYPE_LABEL);
				
				ConceptType(sigType);
			
				// Output defining label for formal parameter.
				corefLabel = definingLabelTable.getNextAvailableDefiningLabel();

				definingLabelTable.mapDefiningLabelToDefiningConcept(corefLabel, formalParams[sig]);
				generate("*" + corefLabel);
				}
			}
			
  	generate(")");
  	
  	// Generate relator (if any).
  	differentia = typeDef.getRelator();
  	if (differentia != null)
  		{
  		// Should probably do some context stuff here to ensure the labels are fresh.
  		Graph(differentia, definingLabelTable);
  		}
  		
  	generate(")");  	
  	}

    /**
     * Generates an arc to the output stream.
     *
     * @param argument  the relation argument to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     */
  final void Arc(Concept argument, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    String corefLabel;
    
    // Check for null argument (incomplete relation)
    if (argument == null)
      {
      if (!allowIncompleteRelations)
      	throw new GeneratorException("Incomplete relation encountered during generation.");
      // If we are allowing incomplete relations then we simply generate nothing for 
      // this argument.
      }
    else
      {
      CoreferenceSet corefSets[];

      // Check if this concept is a defining concept and use the label from it if possible.
      corefLabel = definingLabelTable.getDefiningLabelByDefiningConcept(argument);
	      
      if (corefLabel == null)
      	{
      	// Concept is not a defining concept, check if it has coreference sets
	      
	      corefSets = argument.getCoreferenceSets();

	      if (corefSets == null || corefSets.length == 0)
	        {
    	    // This should never happen unless we are generating less than the entire graph.  
    	    // Otherwise we would have generated the concept earlier, discovered this relator,
    	    // and added it to the table as a defining concept.
    	    // @bug Since this is possible, we might as well write a literal here.  
    	    // Let's do that for now.
    	    // The other case where this can occur is if we allow use of literal concepts
    	    // in relators which exclusively relate said concepts.  In this case, we would
    	    // have deliberately skipped generating the concept, leading us to this point.
    	    // Possibly dangerous if we allow defining label tables to be imported prior
    	    // to generation.
		      Concept(argument, definingLabelTable);
	        }
  	    else
    	    {
      	  corefLabel = definingLabelTable.getDefiningLabelByCoreferenceSet(corefSets[0]);
	        if (corefLabel == null)
    	      {
  	        // Again, this should never happen unless we are generating less than the
  	        // entire graph.  See above.
  	  	    Concept(argument, definingLabelTable);
          	}
        	else
          	generate("?" + corefLabel);
        	}
        }
      else
      	{
       	generate("?" + corefLabel);
      	}
      }
    }
  	
	  /**
     * Generates a relation comment to output stream.
     *
     * @param comment  the relation comment to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void RelationComment(String comment) throws GeneratorException
    {
    if (comment == null)
      return;

    if (comment.length() == 0)
      return;

    generate(";"+escapeCharactersInString(comment, ")", '\\'));
    }

	/* Actor Rules */
	
    /**
     * Generates a actor to the output stream.
     *
     * @param actor  the actor to be generated.
		 * @param definingLabelTable  the current defining label table.
     * @exception GeneratorException  if an error occurs while generating.
     *
     * @bug Literal concept handling is just plain wrong.
     * @bug Must be entirely reexamined and updated based on Relation() method. 
     * (e.g. allowIncompleteRelation)
     */
  void Actor(Actor actor, DefiningLabelTable definingLabelTable) throws GeneratorException
    {
    RelationType relType;
    Concept inArcs[];
    Concept outArcs[];

		// Generate actor start
    generate("<");
   
    // Generate typefield
    relType = actor.getType();
    if (relType != null)
    	RelationType(relType);
    else
      throw new GeneratorException("No type found while generating actor.");

    // Generate input arguments as bound labels
    inArcs = actor.getInputArguments();

    for (int arg = 0; arg < inArcs.length; arg++)
      Arc(inArcs[arg], definingLabelTable);

		// Generate input/output arc delimiter
		generate("|");

    // Generate output arguments as bound labels
    outArcs = actor.getOutputArguments();

    for (int arg = 0; arg < outArcs.length; arg++)
      Arc(outArcs[arg], definingLabelTable);
      
		// Generate actor comment
 		if (!supressNodeComments)
	    ActorComment(actor.getComment());

		// Generate actor terminator
    generate(">");
    }



    /**
     * Generates a actor comment to output stream.
     *
     * @param comment  the actor comment to be generated.
     * @exception GeneratorException  if an error occurs while generating.
     */
  void ActorComment(String comment) throws GeneratorException
    {
    if (comment == null)
      return;

    if (comment.length() == 0)
      return;

    generate(";"+escapeCharactersInString(comment, ">", '\\'));
    }
  }
