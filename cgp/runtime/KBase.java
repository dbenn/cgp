/**
 * A conceptual graph language which embodies Guy Mineau's process formalism.
 * Copyright (C) 2000 David Benn
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
 * A Knowledge Base.
 *
 * This encapsulates Notio's Knowledge Base functionality and adds a 
 * set of structurally equivalent graphs. It may be that one is expected
 * to add everything to nested contexts under the KnowledgeBase's
 * outermost context. I'm not sure this makes sense though. What the
 * graph collection here represents is more like a canon, although no
 * truth maintenance is carried out, e.g. does the collection contain
 * contradictory graphs? This is not a trivial problem however, as Mineau
 * recognises in the closing remarks of his ICCS'98 Processes paper. 
 *
 * This is a subclass of Type purely so it can be made available as
 * a special value for display by pCG programs. The rationale is 
 * that it may prove useful to be able to discover the contents of
 * the knowledge base when a program isn't working as expected.
 * Accordingly, this special value should be updated at the top-level
 * scope.
 *
 * David Benn, August 2000.
 */

package cgp.runtime;

import cgp.runtime.GraphType;
import cgp.runtime.Scope;
import cgp.runtime.Type;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeSet;

import notio.Concept;
import notio.ConceptType;
import notio.ConceptTypeHierarchy;
import notio.Graph;
import notio.KnowledgeBase;
import notio.MatchResult;
import notio.RelationTypeHierarchy;
import notio.RelationType;

public class KBase extends Type {
    // Static fields.
    static private String KBASE_NAME = "_KB";
 
    // Instance fields.
    private KnowledgeBase kb;
    private ConceptTypeHierarchy conceptTypes;
    private RelationTypeHierarchy relationTypes;
    private TreeSet conceptTypeNames;
    private TreeSet relationTypeNames;
    private LinkedList graphs;
    private Namespace corefVars;

    // Constructors.
    public KBase() {
	// New Notio KB.
	kb = new KnowledgeBase();

	// Create type hierarchies.
	conceptTypes = kb.getConceptTypeHierarchy();
	conceptTypes.setCaseSensitiveLabels(true);
	relationTypes = kb.getRelationTypeHierarchy();
	relationTypes.setCaseSensitiveLabels(true);

	// Create repository for type names. They will be used to
	// look up types in the hierarchies.
	conceptTypeNames = new TreeSet();
	relationTypeNames = new TreeSet();

	// Add built-in concept types.
	addConceptType("Number");
	addConceptType("String");
	addConceptType("Boolean");
	addConceptType("Proposition");
	addConceptType("Erasure");
	addConceptType("Condition");

	// Create an empty set of structurally equivalent graphs.
	graphs = new LinkedList();

	// Create a respository for coreferent variable mappings.
	// This is a restricted form of corefernce sets, designed
	// to be able to capture information from name designators
	// when asserting graphs into a knowledge base, especially
	// during process execution.
	corefVars = new Namespace();

	// Localise special _KB variable.
	Type.getScopeStack().peek().def(KBASE_NAME, this);

	setType("KnowledgeBase");
    }

    public KBase(KBase otherKBase) {
	// Create a new KBase by copying an existing one.

	// Copy the Notio KnowledgeBase.
	//
	// Does the following *really* copy the KnowledgeBase
	// components? (TBD)
	//
	// Only the marker set could be mutated in pCG since
	// type declarations are only permitted as top-level
	// statements, not within functions or processes.
	KnowledgeBase otherKB = otherKBase.getKB();
	kb = new KnowledgeBase(otherKB.getConceptTypeHierarchy(),
			       otherKB.getRelationTypeHierarchy(),
			       otherKB.getMarkerSet(),
			       otherKB.getOutermostContext());

	// Get type hierarchies.
	conceptTypes = kb.getConceptTypeHierarchy();
	relationTypes = kb.getRelationTypeHierarchy();

	// Copy the type names.
	// These don't have to be cloned since they won't be mutated.
	conceptTypeNames = new TreeSet();
	conceptTypeNames.addAll(otherKBase.getConceptTypeNames());
	relationTypeNames = new TreeSet();
	relationTypeNames.addAll(otherKBase.getRelationTypeNames());
	
	// Copy the graph set.
	// Graphs in this set *might* be mutated.
	graphs = new LinkedList();
	LinkedList otherGraphs = otherKBase.getAssertedGraphs();
	for (int i=0;i<otherGraphs.size();i++) {
	    graphs.add(new GraphType(((GraphType)otherGraphs.get(i)).
				     getValue()));
	}

	// Get a copy of the coreference variables so others can
	// be added to this KBase without contaminating the original.
	corefVars = otherKBase.getCorefVars().copy();

	// Localise special _KB variable.
	Type.getScopeStack().peek().def(KBASE_NAME, this);

	setType("KnowledgeBase");	
    }

    // Private methods.

    private String describeConceptTypes() {
	String str = "";
	Iterator it = conceptTypeNames.iterator();
	
	while (it.hasNext()) {
	    String name = (String)it.next();
	    ConceptType ct = conceptTypes.getTypeByLabel(name);

	    str += name + " has";

	    ConceptType[] superTypes = ct.getImmediateSuperTypes();
	    if (superTypes.length == 0) {
		str += " no supertypes"; 
	    } else {
		str += " supertypes ";
		for (int i=0;i<superTypes.length;i++) {
		    str += superTypes[i].getLabel();
		    if (i < superTypes.length-1) {
			str += ", ";
		    }
		}
	    }

	    str += " and";

	    ConceptType[] subTypes = ct.getImmediateSubTypes();
	    if (subTypes.length == 0) {
		str += " no subtypes"; 
	    } else {
		str += " subtypes ";
		for (int i=0;i<subTypes.length;i++) {
		    str += subTypes[i].getLabel();
		    if (i < subTypes.length-1) {
			str += ", ";
		    }
		}
	    }

	    str += ".\n";
	}

	return str;
    }

    private String describeRelationTypes() {
	String str = "";
	Iterator it = relationTypeNames.iterator();
	
	while (it.hasNext()) {
	    String name = (String)it.next();
	    RelationType ct = relationTypes.getTypeByLabel(name);

	    str += name + " has";

	    RelationType[] superTypes = ct.getImmediateSuperTypes();
	    if (superTypes.length == 0) {
		str += " no supertypes"; 
	    } else {
		str += " supertypes ";
		for (int i=0;i<superTypes.length;i++) {
		    str += superTypes[i].getLabel();
		    if (i < superTypes.length-1) {
			str += ", ";
		    }
		}
	    }

	    str += " and";

	    RelationType[] subTypes = ct.getImmediateSubTypes();
	    if (subTypes.length == 0) {
		str += " no subtypes "; 
	    } else {
		str += " subtypes ";
		for (int i=0;i<subTypes.length;i++) {
		    str += subTypes[i].getLabel();
		    if (i < subTypes.length-1) {
			str += ", ";
		    }
		}
	    }

	    str += ".\n";
	}

	return str;
    }

    /**
     * Return the specified graph's index in the list
     * or -1 if it is not present. The search is based
     * upon structural graph equivalence.
     */
    private int graphLocation(GraphType g) {
	int index = -1;

	for (int i=0;i<graphs.size();i++) {
	    GraphType h = (GraphType)graphs.get(i);
	    if (g.equals(h)) {
		index = i;
		break;
	    }
	}

	return index;
    }

    // Public methods.

    public KnowledgeBase getKB() {
	return kb;
    }

    public LinkedList getAssertedGraphs() {
	return graphs;
    }

    public Namespace getCorefVars() {
	return corefVars;
    }

    public TreeSet getConceptTypeNames() {
	return conceptTypeNames;
    }

    public TreeSet getRelationTypeNames() {
	return relationTypeNames;
    }

    public String toString() {
	// Might want to split out types from graphs later
	// and add marker set.
	return "ACTIVE KNOWLEDGE BASE\n\n" + 
	       "Concept Types\n" +
	       "-------------\n" +
	       describeConceptTypes() + "\n" +

	       "Relation Types\n" +
	       "--------------\n" +
	       describeRelationTypes() + "\n" +

	       "Graphs\n" +
	       "------\n" +
	       graphs + "\n";
    }

    /**
     * Add a single concept type to the hierarchy if it has not
     * already been added.
     */
    public void addConceptType(String typeName) {
	ConceptType ct = conceptTypes.getTypeByLabel(typeName);

	if (ct == null) {
	    conceptTypeNames.add(typeName);
	    conceptTypes.addTypeToHierarchy(new ConceptType(typeName));
	}
    }

    /**
     * Add a concept type to the hierarchy if it has not
     * already been added, and add an immediate supertype.
     * It is a precondition that the supertype has already
     * been added via addConceptType(String). 
     */
    public void linkConceptTypes(String superTypeName, String typeName) {
	ConceptType ct = conceptTypes.getTypeByLabel(typeName);

	if (ct == null) {
	    conceptTypeNames.add(typeName);
	    ct = new ConceptType(typeName);
	    conceptTypes.addTypeToHierarchy(ct);
	}

	ConceptType sct = conceptTypes.getTypeByLabel(superTypeName);
	conceptTypes.addSuperTypeToType(ct, sct);
    }

    /**
     * Add a single relation type to the hierarchy if it has not
     * already been added.
     */
    public void addRelationType(String typeName) {
	RelationType rt = relationTypes.getTypeByLabel(typeName);

	if (rt == null) {
	    relationTypeNames.add(typeName);
	    relationTypes.addTypeToHierarchy(new RelationType(typeName));
	}
    }

    /**
     * Add a relation type to the hierarchy if it has not
     * already been added, and add an immediate supertype.
     * It is a precondition that the supertype has already
     * been added via addRelationType(String). 
     */
    public void linkRelationTypes(String superTypeName, String typeName) {
	RelationType rt = relationTypes.getTypeByLabel(typeName);

	if (rt == null) {
	    relationTypeNames.add(typeName);
	    rt = new RelationType(typeName);
	    relationTypes.addTypeToHierarchy(rt);
	}

	RelationType srt = relationTypes.getTypeByLabel(superTypeName);
	relationTypes.addSuperTypeToType(rt, srt);
    }

    /**
     * Bind coreference variables in the specified graph, if possible,
     * by reference to the current knowledge base.
     */
    public void bindCorefVars(GraphType g) {
	LinkedList conceptList = g.getConcepts().getValue();
	cgp.runtime.ConceptType[] concepts =
	    (cgp.runtime.ConceptType[])conceptList.
	    toArray(new cgp.runtime.ConceptType[0]);
	for (int i=0;i<concepts.length;i++) {
	    if (concepts[i].hasVarDesignator()) {
		String name =
		    ((StringType)concepts[i].getDesignatorValue()).getValue();
		Type value = this.getCorefVarValue(name);
		if (!(value instanceof UndefinedType)) {
		    concepts[i].setDesignatorValue(value);
		} 
	    }
	}
    }

    /**
     * Assert a graph in this knowledge base unless a
     * structurally equivalent graph has already been 
     * asserted.
     *
     * Coreferent variables in the graph are optionally first bound,
     * a copy being taken to prevent side effects on the original
     * graph.
     *
     * Markers in the graph's concepts are added to the 
     * knowledge base's marker set (TBD). May not be 
     * necessary if all marker comparison is done by
     * direct comparison.
     */
    public void assertGraph(GraphType g, boolean bindVars) {
	if (bindVars) {
	    g = g.copy();
	    bindCorefVars(g);
	}
	if (graphLocation(g) == -1) {
	    graphs.add(g);
	}
    }

    /**
     * Retract a graph from this knowledge base based
     * upon structural equivalence.
     *
     * Coreferent variables in the graph are optionally first bound,
     * a copy being taken to prevent side effects on the original
     * graph.
     *
     * Markers in the graph's concepts are removed from 
     * the knowledge base (TBD). May not be necessary if 
     * all marker comparison is done by direct comparison.
     */
    public void retract(GraphType g, boolean bindVars) {
	if (bindVars) {
	    g = g.copy();
	    bindCorefVars(g);
	}
	int index = graphLocation(g);
	if (index >= 0) {
	    graphs.remove(index);
	}
    }

    /**
     * Assert a graph, first binding coreference variables.
     */
    public void assertGraph(GraphType g) {
	assertGraph(g, true);
    }

    /**
     * Retract a graph, first binding coreference variables.
     */
    public void retract(GraphType g) {
	retract(g, true);
    }
    
    /**
     * Does the specified graph exactly match a graph in the set?
     */
    public boolean exactMatch(GraphType g) {
	return graphLocation(g) >= 0;
    }

    /**
     * Does the specified graph project onto a graph in the set?
     */
    public GraphType projectionMatch(GraphType g) {
	Graph p = null;
	Graph source = g.getValue();

	for (int i=0;i<graphs.size();i++) {
	    GraphType h = (GraphType)graphs.get(i);
	    p = h.project(source);
	    if (p != null) {
		break;
	    }
	}

	return p != null ? new GraphType(p) : null;
    }

    /**
     * Add a coreferent variable mapping.
     */
    public void addCorefVarMapping(String name, Type value) {
	corefVars.def(name, value);
    }

    /**
     * Get a coreferent variable's value.
     * May return undefined.
     */
    public Type getCorefVarValue(String name) {
	return corefVars.get(name);
    }

   // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public ListType getGraphs() {
	return new ListType(graphs);
    }

    public StringType getConcepttypes() {
	return new StringType(describeConceptTypes());
    }

    public StringType getRelationtypes() {
	return new StringType(describeRelationTypes());
    }

    public ListType getCorefvars() {
	return new ListType(corefVars.getMappings());
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */
}
