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
 * Graph type class for pCG expressions.
 *
 * The KB where a concept instance is created determines what types (the same
 * for all KBs), markers, and graphs are available to it. Whether this always 
 * makes sense remains to be seen.
 * 
 * David Benn, June-October 2000, June 2001
 */

package cgp.runtime;

import cgp.CGP;

import cgp.runtime.GraphException;
import cgp.runtime.KBase;
import cgp.runtime.ListType;
import cgp.runtime.NumberType;
import cgp.runtime.StringType;
import cgp.runtime.Type;
import cgp.runtime.UndefinedType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.LinkedList;

import notio.Actor;
import notio.Concept;
import notio.CopyingScheme;
import notio.Generator;
import notio.GeneratorException;
import notio.Graph;
import notio.JoinException;
import notio.KnowledgeBase;
import notio.MatchingScheme;
import notio.MatchResult;
import notio.ParserException;
import notio.Referent;
import notio.Relation;
import notio.RelationType;
import notio.TranslationContext;
//import notio.translators.LFParser;
import notio.translators.CGIFParser;
import notio.translators.LFGenerator;

public class GraphType extends Type {
    // Static fields.
    public static CopyingScheme COPYING_SCHEME = 
	new CopyingScheme(CopyingScheme.GR_COPY_DUPLICATE,
			  CopyingScheme.CN_COPY_DUPLICATE,
			  CopyingScheme.RN_COPY_DUPLICATE,
			  CopyingScheme.DG_COPY_DUPLICATE,
			  CopyingScheme.COMM_COPY_ON,
			  null);

    public static CopyingScheme COPYING_SCHEME_NO_COMMENTS = 
	new CopyingScheme(CopyingScheme.GR_COPY_DUPLICATE,
			  CopyingScheme.CN_COPY_DUPLICATE,
			  CopyingScheme.RN_COPY_DUPLICATE,
			  CopyingScheme.DG_COPY_DUPLICATE,
			  CopyingScheme.COMM_COPY_OFF,
			  null);

    public static MatchingScheme MATCH_EXACT =
	new MatchingScheme(MatchingScheme.GR_MATCH_COMPLETE,

			   // The following used to work, but with the
			   // new CGIF parser (cgp.translators.CGIFParser)
			   // it stopped working. Haven't had time to
			   // figure out why though. Instead, check types
			   // and "manually" check for concept equivalence
			   // for now. This may be due to the fact that some
			   // descriptors are empty graphs, while some are
			   // UndefinedType.undefined. See also
			   // GraphType.equals().
		       //MatchingScheme.CN_MATCH_ALL, // or match types?

			   MatchingScheme.CN_MATCH_TYPES,

			   MatchingScheme.RN_MATCH_TYPES,
			   MatchingScheme.CT_MATCH_LABEL,
			   MatchingScheme.RT_MATCH_LABEL,
			   MatchingScheme.QF_MATCH_ANYTHING, // only option!
			   MatchingScheme.DG_MATCH_ANYTHING, // extra check!
			   MatchingScheme.MARKER_MATCH_ANYTHING, // ditto
			   MatchingScheme.ARC_MATCH_CONCEPT,
			   MatchingScheme.COREF_AUTOMATCH_OFF,
			   MatchingScheme.COREF_AGREE_OFF,
			   MatchingScheme.FOLD_MATCH_OFF,
			   MatchingScheme.CONN_MATCH_ON,
			   1, // maximum matches permitted 
			   null, // no marker comparator
			   null); // use current scheme for nested contexts

    public static MatchingScheme MATCH_SUBGRAPH =
	new MatchingScheme(MatchingScheme.GR_MATCH_SUBGRAPH,
			   MatchingScheme.CN_MATCH_ALL, // or match types?
			   MatchingScheme.RN_MATCH_TYPES,
			   MatchingScheme.CT_MATCH_LABEL,
			   MatchingScheme.RT_MATCH_LABEL,
			   MatchingScheme.QF_MATCH_ANYTHING, // only option!
			   MatchingScheme.DG_MATCH_ANYTHING, // extra check!
			   MatchingScheme.MARKER_MATCH_ANYTHING, // ditto
			   MatchingScheme.ARC_MATCH_CONCEPT,
			   MatchingScheme.COREF_AUTOMATCH_OFF,
			   MatchingScheme.COREF_AGREE_OFF,
			   MatchingScheme.FOLD_MATCH_OFF,
			   MatchingScheme.CONN_MATCH_ON,
			   1, // maximum matches permitted 
			   null, // no marker comparator
			   null); // use current scheme for nested contexts

    // ** TBD: Finnegan Southey claims that Notio can handle projections
    // ** so long as folding is not required. See his FAQ on the Notio web 
    // ** site. Perhaps this is faster than my method below? Future work?

    // ** TBD: be sure to document in the Makefile and in the thesis which
    // ** version of Notio (and ANTLR etc) is required.

    // Instance fields.
    private Graph value;
    private KBase kbase; // the KB with which this graph is associated

    // Constructors.
    public GraphType(Graph g) {
	kbase = Type.getKBStack().peek(); // where am I *now*
	value = g.copy(COPYING_SCHEME);
	setType("graph");
    }

    public GraphType(String s) {
	kbase = Type.getKBStack().peek(); // where am I *now*
	value = parseGraph(s).copy(COPYING_SCHEME);
	setType("graph");
    }

    public GraphType(File f) {
	kbase = Type.getKBStack().peek(); // where am I *now*
	value = parseGraph(f).copy(COPYING_SCHEME);
	setType("graph");
    }

    // The purpose of this constructor is to be used in conjunction
    // with matchFound() called by processCall() in the interpreter
    // where multiple projections are required for a single graph
    // with actors present. This constructor creates a new graph
    // from a single relation, and its arguments.
    public GraphType(Relation r) {
	kbase = Type.getKBStack().peek(); // where am I *now*

	// Create a graph from an existing relation.
	// Copies must be made of the relation and its
	// arguments however, otherwise Notio will
	// complain that they already belong to another
	// graph, which is fair enough! We want to avoid
	// side effects anyway. The referent is also
	// copied, to avoid side effects.
	Concept[] args = r.getArguments();
	Concept[] newArgs = new Concept[args.length];
	for (int i=0;i<args.length;i++) {
	    Referent newReferent = 
		args[i].getReferent().copy(ConceptType.COPYING_SCHEME);
	    newArgs[i] = new Concept(args[i].getType(), newReferent);
	}
	value = new Graph();
	value.addRelation(new Relation(r.getType(), newArgs));

	setType("graph");	
    }

    // Public methods.

    public Graph getValue() {
	return value;
    }

    public KBase getKBase() {
	return kbase;
    }

    public boolean equals(Object other) {
	
	Graph otherGraph = ((GraphType)other).getValue();
	if (other instanceof GraphType) {
	    boolean equal = false;

	    // First check whether both graphs are blank (i.e. have no
	    // relations and concepts). This might include one being
	    // null and the other being empty (i.e. null vs new Graph()).
	    // This may be the difference between cgp.translators.CGIFParser
	    // setting an empty graph as a referent, and a new concept being
	    // created internally by pCG with a null referent. This should
	    // be sorted out sometime. See also ConceptType.equals() re: these
	    // cases.
	    if (value.isBlank() && otherGraph.isBlank()) {
		return true;
	    }

	    // Next, check for an exact match.
	    MatchResult result = Graph.matchGraphs(value, otherGraph, 
						   MATCH_EXACT);
	    equal = result.matchSucceeded();

	    /*
	    // Finally, check for equivalent designators since this
	    // wasn't included in the exact match procedure.
	    if (equal) {
		equal = areDesignatorsEquivalent(otherGraph);
	    }
	    */

	    // Check for concept equality. Two graphs will have identical 
	    // relations, including arguments. Checking for designator
	    // equality isn't enough!
	    if (equal) {
		Concept[] c1 = value.getConcepts(); 
		Concept[] c2 = otherGraph.getConcepts();
		if (c1.length != c2.length) {
		    equal = false;
		} else {
		    for (int i=0;i<c1.length;i++) {
			ConceptType ct1 = new ConceptType(c1[i]);
			ConceptType ct2 = new ConceptType(c2[i]);
			if (!ct1.equals(ct2)) {
			    equal = false;
			    break;
			}
		    }
		}
	    }

	    return equal;
	} else {
	    return false;
	}
    }

    /** 
     * Check designator and marker equivalence together
     * by iterating over each concept's designator (which
     * consists of a literal, marker, or name) and testing
     * pCG value equivalence. If there is no designator,
     * the value will be "undefined".
     */
    public boolean areDesignatorsEquivalent(Graph other) {
	boolean equal = true;

	Concept[] firstConcepts = value.getConcepts();
	Concept[] secondConcepts = other.getConcepts();
	
	if (firstConcepts.length == secondConcepts.length) {
	    for (int i=0;i<firstConcepts.length;i++) {
		Type firstValue;
		firstValue = new ConceptType(firstConcepts[i]).
		    getDesignatorValue();
		
		Type secondValue;
		secondValue = new ConceptType(secondConcepts[i]).
		    getDesignatorValue();
		
		if (!firstValue.equals(secondValue)) {
		    equal = false;
		    break;
		}
	    }
	} else {
	    equal = false;
	}
	
	return equal;
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof GraphType) {
	    result = new BooleanType(this.equals(other));
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public String toString() {
	StringWriter w = new StringWriter();
	KnowledgeBase kb = kbase.getKB();
	TranslationContext tc = new TranslationContext();

	try {
	    Generator gen = null;
	    if (cgp.CGP.LFOpt) {
		gen = new LFGenerator();
	    } else {
		// CGIF.
		try {
		    gen = (Generator)
			(Class.forName(cgp.CGP.CGIFGenOpt)).newInstance();
		} catch (Exception e) {
		    String msg = "error creating CGIF generator " + 
			         cgp.CGP.CGIFGenOpt;
		    throw new GraphException(msg);
		}
	    }
	    gen.initializeGenerator(w, kb, tc);
	    gen.generateGraph(value);
	} catch (Exception e) {
	    throw new GraphException("error generating graph: " + 
				       w.toString().trim());
	}
 
	return w.toString().trim();
    }    

    /** 
     * Does this graph contain one or more actor relations?
     * This asks nothing about the validity of any such actors,
     * a task carried out at pCG actor definition time.
     */
    public boolean containsActorNodes() {
	return value.getActorRelations().length > 0;
    }

    public Graph parseGraph(Object o) {
	Graph g = null;

	// Try parsing LF first, and if that fails, CGIF.
	// Notio's LF parsing is somewhat broken, so except
	// for simple graphs, CGIF is more reliable currently.
	// It would have been better to just pass a Reader
	// object to this method, but not all Reader subclasses
	// support stream resetting which is necessary when
	// LF parsing fails, and CGIF parsing must be attempted.
	// Probably ought to attempt a CGIF parse before LF,
	// since the former is more likely to be used in pCG
	// programs, so this would result in a speed-up.
	try {
	    // File or string reader?
	    Reader r;
	    if (o instanceof File) {
		r = new FileReader((File)o);
	    } else if (o instanceof String) {
		r = new StringReader((String)o);
	    } else {
		throw new GraphException("error parsing graph: can only " +
					   "use string or file");
	    }
	    /*
	    LFParser par = new LFParser();
	    KnowledgeBase kb = kbase.getKB();
	    TranslationContext tc = new TranslationContext();
	    par.initializeParser(r, kb, tc);
	    g = par.parseGraph();
	    */
	    throw new ParserException("Ignore LF to avert Win32 JIT error.");
	} catch (FileNotFoundException e) {
	    throw new GraphException("graph file '" + ((File)o).getPath() +
				       "' not found.");
	} catch (ParserException e1) {	
	    try {
		KnowledgeBase kb = kbase.getKB();
		TranslationContext tc = new TranslationContext();

		// Let a factory method figure out what to do with
		// the object which is the purported CG source, and
		// create the currently set CGIF parser.
		notio.Parser parser;
		if (o instanceof File) {
		    parser = cgp.CGP.createCGParser(cgp.CGP.CGIFParserOpt, 
						    (File)o, kb, tc);
		} else if (o instanceof String) {
		    parser = cgp.CGP.createCGParser(cgp.CGP.CGIFParserOpt, 
						    (String)o, kb, tc);
		} else {
		    throw new GraphException("error parsing graph: can only"+
					     "use string or file");
		}

		g = parser.parseGraph();
	    } catch (Exception e2) {
		System.out.println(e2);
		throw new GraphException("error parsing graph as LF/CGIF");
	    }
	}

	return g;
    }

    /**
     * Are the specified concepts type compatible? Note that this does not
     * mean that they are necessarily restriction compatible. Concepts in
     * the first array must be the same type or supertypes of those in the
     * second. The number of concepts in each array must be the same.
     */
    private boolean areConceptsCompatible(Concept[] c1, Concept[] c2) {
	if (c1.length != c2.length) {
	    return false;
	} else {
	    for (int i=0;i<c1.length;i++) {
		if (!c1[i].getType().hasSubType(c2[i].getType())) {
		    return false;
		}		
	    }
	}
	return true;
    }

    /**
     * Is the first conceptual relation supertype compatible with the second?
     * For this to be true, the first's relation must be the same type or a 
     * supertype of the second, and the same must hold for each concept
     * argument of the two conceptual relations. The number of arguments
     * must also be the same as must the arc directionality of the arguments.
     * The test here is for signature compatibility only, not restrictability.
     */
    private boolean areRelationsCompatible(Relation r1, Relation r2) {
	if (!r1.getType().hasSubType(r2.getType())) {
	    return false;
	}

	if (!areConceptsCompatible(r1.getInputArguments(),
				   r2.getInputArguments())) {
	    return false;
	}

	return areConceptsCompatible(r1.getOutputArguments(),
				     r2.getOutputArguments());
    }

    /**
     * Project the specified graph onto this graph, returning the
     * projection as a new graph, or null. The parameter can be
     * seen as a filter which will be applied to this graph. The
     * projection will only be successful if the filter graph is
     * a supertype of this graph, i.e. all relations and
     * concepts in this graph are subtypes or the same as those 
     * in the filter graph, and the filter's concepts are restrictable 
     * by type and/or referent to the target's. Note that the
     * filter graph may have more or less nodes than the target.
     */
    public Graph project(Graph filter) {
	// Get the relations in this graph and the count of same.
	Relation[] targetRelations = value.getRelations();
	int targetCount = value.getNumberOfRelations();

	// Get the relations in the filter graph and the count of same.
	// Don't actually need filterRelations, just projectionRelations,
	// but the code comprehesibility is arguably enhanced.
	Relation[] filterRelations = filter.getRelations();
	int filterCount = filter.getNumberOfRelations();

	// Copy this graph for mutation as resulting projected graph,
	// and get the relations of this new graph. This graph must
	// always be seen as the template for the final projection
	// since it may contain desired actors or extra relations.
	Graph projection = filter.copy(COPYING_SCHEME);
	Relation[] projectionRelations = projection.getRelations();

	if (CGP.isTraceOn()) {
	    System.out.println("TRACE: projection = filter -> " + 
			       new GraphType(projection));
	}

	// Find the relations in the filter graph which match those
	// in the target graph. If a target relation has no match,
	// the match array element defaults to null. An important
	// thing to note here is that only the relations in the target 
	// graph need to be matched against. This makes it possible
	// to have extra relations (such as actors) in the filter,
	// making the filter more specialised than the target with
	// respect to *number* of relations. However, if one graph is 
	// more specialised than the other with respect to type or 
	// referent restriction, it must always be the target.
	//
	// Note: We're in trouble if one graph contains more than one 
	// compatible conceptual relation, e.g. [Person: X]->(Has)->[Friend: A]
	// and [Person: X]->(Has)->[Friend: B]. Perhaps this is an argument
	// for doing restriction at this point of the algorithm, rather than
	// later.
	Relation[] matchingRelations = new Relation[targetCount];
	int matches = 0;
	for (int i=0;i<targetRelations.length;i++) {
	    for (int j=0;j<filterRelations.length;j++) {
		if (areRelationsCompatible(filterRelations[j],
					   targetRelations[i])) {
		    // This matched relation may later be mutated.
		    matchingRelations[i] = projectionRelations[j];
		    matches++;
		}
	    }
	}

	if (CGP.isTraceOn()) {
	    System.out.println("");
	    for (int i=0;i<matchingRelations.length;i++) {
		System.out.println("TRACE: " + i + ": target " + 
				   targetRelations[i].getType().getLabel() +
				   " matches " +
				   (matchingRelations[i] == null ? "nothing" :
				   matchingRelations[i].getType().getLabel()));
		
	    }
	}

	// To proceed, there MUST be as many matching relations as there
	// are relations in the smallest graph (i.e. the graph with the 
	// least number of relations). Another implication is that in order 
	// to do a projection under pCG at all, the graphs in question must 
	// contain relations! That's a limitation since one may want to 
	// search for a particular concept only. Arguably, projection
	// is a little too high-powered for that: try a join instead. Still,
	// it may be a valid thing to want to do.
	int leastCount = targetCount < filterCount ? targetCount : filterCount;

	if (CGP.isTraceOn()) {
	    System.out.println("matches: " + matches);
	    System.out.println("least count: " + leastCount);
	}

	if (matches == 0 || leastCount != matches) {
	    // No valid projection.
	    return null;
	}

	// For each non-null relation in match array:
	//
	//  - Valence must be the same as for target relation (done above).
	//  - Relation type must be restrictable to target's type.
	//  - Each concept must be same type as or restrictable to target.
	//
	// TBD: Could do all of this in above loop, and simply check number
	//      of relations afterward to determine whether projection was 
	//      valid.

	for (int i=0;i<matchingRelations.length;i++) {
	    Relation match = matchingRelations[i];
	    if (match != null) {
		Relation target = targetRelations[i];
		
		// Restrict filter relation type to target's type
		// if the latter is a proper subtype of the former.
		if (match.getType().hasProperSubType(target.getType())) {
		    match.setType(target.getType());
		}
		
		// Restrict each concept in the matched relation to
		// conform to the target relation, if necessary. Note
		// the importance of already having established equal
		// valence.
		Concept[] matchArgs = match.getArguments();
		Concept[] targetArgs = target.getArguments();
		for (int j=0;j<matchArgs.length;j++) {
		    ConceptType matchArg = new ConceptType(matchArgs[j]);
		    ConceptType targetArg = new ConceptType(targetArgs[j]);
		    if (!matchArg.equals(targetArg)) {
			if (!matchArg.restriction(targetArg)) {
			    // No valid projection.
			    return null;
			}
		    }
		    if (CGP.isTraceOn()) {
			System.out.println("TRACE: current projection -> " + 
					   new GraphType(projection));
		    }
		}
	    }
	}

	return projection;
    }

    /**
     * Attempt to join the specified graph onto this one.
     * Currently, the operation consists of a search for
     * a concept in the two graphs which is identical, and
     * there is no type restriction performed. Once identified,
     * the graphs are joined on the two concepts.
     *
     * TBD: Probably could rely upon matching scheme to find
     *      compatible concepts.
     */
    public Graph join(Graph g) {
	Graph newGraph = null;
	Concept[] concepts = value.getConcepts();
	Concept[] otherConcepts = g.getConcepts();
	Concept target = null;
	Concept source = null;

	for (int i=0;i<otherConcepts.length;i++) {
	    ConceptType other = new ConceptType(otherConcepts[i]);

	    for (int j=0;j<concepts.length;j++) {
		ConceptType concept = new ConceptType(concepts[j]);
		if (concept.equals(other)) {
		    target = concepts[j];
		    source = otherConcepts[i];
		    break;
		}
	    }

	    if (target != null) {
		break;
	    }
	}

	try {
	    newGraph = Graph.join(value, target, g, source,
				  MATCH_EXACT, COPYING_SCHEME);
	} catch (JoinException e) {
	    // The new graph has a default value of null.
	}

	return newGraph;
    }

    /**
     * Attempt to join the specified graph onto this one
     * at their heads. This assumes that the concepts at
     * the heads are identical. If not, the join will fail. 
     * The head concept of a graph is taken to be the
     * zeroth concept insofar as Notio ordering is concerned.
     * 
     * TBD: A logical intermediate would be a method which permits
     *      a concept to be specified as the target concept. There
     *      may of course be more than one of the same concept in a
     *      given graph though.
     */
    public Graph joinAtHead(Graph g) {
	Graph newGraph = null;
	Concept[] concepts = value.getConcepts();
	Concept[] otherConcepts = g.getConcepts();

	if (concepts.length > 0 && otherConcepts.length > 0) {
	    Concept target = null;
	    Concept source = null;
	    ConceptType other = new ConceptType(otherConcepts[0]);
	    ConceptType concept = new ConceptType(concepts[0]);
	    if (concept.equals(other)) {
		target = concepts[0];
		source = otherConcepts[0];
	    }

	    if (target != null) {
		try {
		    newGraph = Graph.join(value, target, g, source,
					  MATCH_EXACT, COPYING_SCHEME);
		} catch (JoinException e) {
		    // The new graph has a default value of null.
		}
	    }
	}

	return newGraph;
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */

    /**
     * Returns a list of all concepts in this graph.
     */
    public ListType getConcepts() {
	Concept[] concepts = value.getConcepts();
	ListType conceptList = new ListType();

	for (int i=0;i<concepts.length;i++) {
	    conceptList.append(new ConceptType(concepts[i]));
	}

	return conceptList;
    }

    /**
     * Returns a list of lists each of which is headed by a relation
     * name, followed by a list of input concepts, and a list of output
     * concepts.
     */
    public ListType getRelations() {
	Relation[] relations = value.getRelations();
	ListType relationList = new ListType();

	for (int i=0;i<relations.length;i++) {
	    // Create list for relation type name, input, output concepts.
	    ListType list = new ListType();

	    // Get relation type name and add it to the list.
	    String relationType = relations[i].getType().getLabel();
	    list.append(new StringType(relationType));

	    // Add a list of input concepts to the list.
	    Concept[] inputArgs = relations[i].getInputArguments();
	    ListType inputArgList = new ListType();
	    for (int j=0;j<inputArgs.length;j++) {
		inputArgList.append(new ConceptType(inputArgs[j]));
	    }
	    list.append(inputArgList);

	    // Add a list of output concepts to the list.
	    Concept[] outputArgs = relations[i].getOutputArguments(); 
	    ListType outputArgList = new ListType();
	    for (int j=0;j<outputArgs.length;j++) {
		outputArgList.append(new ConceptType(outputArgs[j]));
	    }
	    list.append(outputArgList);

	    // Add the <relation-type, input-args, output-args> 
	    // tuple to the list of conceptual relations.
	    relationList.append(list);
	}

	return relationList;
    }

    /**
     * Returns a list of lists each of which is headed by a actor
     * name, followed by a list of input concepts, and a list of output
     * concepts.
     */
    public ListType getActors() {
	Relation[] actors = value.getRelations();
	ListType actorList = new ListType();

	for (int i=0;i<actors.length;i++) {
	    if (actors[i] instanceof Actor) {
		// Create list for actor type name, input, output concepts.
		ListType list = new ListType();
		
		// Get actor type name and add it to the list.
		String actorType = actors[i].getType().getLabel();
		list.append(new StringType(actorType));
		
		// Add a list of input concepts to the list.
		Concept[] inputArgs = actors[i].getInputArguments();
		ListType inputArgList = new ListType();
		for (int j=0;j<inputArgs.length;j++) {
		    inputArgList.append(new ConceptType(inputArgs[j]));
		}
		list.append(inputArgList);
		
		// Add a list of output concepts to the list.
		Concept[] outputArgs = actors[i].getOutputArguments(); 
		ListType outputArgList = new ListType();
		for (int j=0;j<outputArgs.length;j++) {
		    outputArgList.append(new ConceptType(outputArgs[j]));
		}
		list.append(outputArgList);
		
		// Add the <actor-type, input-args, output-args> 
		// tuple to the list of conceptual actors.
		actorList.append(list);
	    }
	}

	return actorList;
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */

    /**
     * Make a copy of this graph.
     */
    public GraphType copy() {
	return new GraphType(value.copy(COPYING_SCHEME));
    }

    /**
     * Make a copy of this graph without comments.
     */
    public GraphType nocomments() {
	return new GraphType(value.copy(COPYING_SCHEME_NO_COMMENTS));
    }

    /**
     * Add to a copy of this graph and return it. Relations and concepts
     * are copied to avoid *AddError exceptions. Rules of Inference
     * such as iteration require that a graph component can be duplicated.<p>
     *
     * This may be too minimal, i.e. may require more checks.
     * Indeed, this function is considered very experimental.
     */
    public Type add(GraphType extraGraph) {
	Type result = UndefinedType.undefined;
	
	if (extraGraph != null) {
	    Graph extra = extraGraph.getValue();

	    if (!extra.isBlank()) {
		Graph newG = value.copy(COPYING_SCHEME);
		
		// Get new graph components.
		String[] comments = extra.getComments();
		Relation[] rels = extra.getRelations();
		Concept[] cons = extra.getConcepts();
		
		// Add comments to new graph.
		for (int i=0;i<comments.length;i++) {
		    newG.addComment(comments[i]);
		}
		
		// Add relations, then singleton graphs (concepts)
		// to avoid adding relation-embedded concepts.
		for (int i=0;i<rels.length;i++) {
		    newG.addRelation(rels[i].copy(COPYING_SCHEME));
		}

		for (int i=0;i<cons.length;i++) {
		    boolean found = false;
		    for (int j=0;j<rels.length;j++) {
			Concept[] args = rels[j].getArguments();
			for (int k=0;k<args.length;k++) {
			    if (args[k] == cons[i]) {
				found = true;
				break;
			    }
			}
		    }
		    if (!found) {
			// If this concept is not in a relation, add it.
			newG.addConcept(cons[i].
					copy(ConceptType.COPYING_SCHEME));
		    }
		}

		result = new GraphType(newG);
	    }
	}

	return result;
    }
 
    /**
     * Project the specified graph onto this graph, returning the
     * projection as a new graph, or undefined. The parameter
     * can be seen as a filter which will be applied to this
     * graph.
     *
     * @see GraphType.project(Graph)
     */
    public Type project(GraphType filterGraph) {
	Graph projection = project(filterGraph.getValue());
	Type result = UndefinedType.undefined;
	if (projection != null) {
	    result = new GraphType(projection);
	}
	return result;
    }

    /**
     * Join operation.
     * Attempt to join the specified graph onto this graph and
     * return the result.
     *
     * @see GraphType.join(Graph)
     */ 
    public Type join(GraphType g) {
	Graph newGraph = join(g.getValue());
	Type result = UndefinedType.undefined;
	if (newGraph != null) {
	    result = new GraphType(newGraph);
	}
	return result;
    }

    /**
     * Join at head concepts operation.
     * Attempt to join the specified graph onto this graph and
     * return the result.
     *
     * @see GraphType.joinAtHead(Graph)
     */ 
    public Type joinAtHead(GraphType g) {
	Graph newGraph = joinAtHead(g.getValue());
	Type result = UndefinedType.undefined;
	if (newGraph != null) {
	    result = new GraphType(newGraph);
	}
	return result;
    }    
}
