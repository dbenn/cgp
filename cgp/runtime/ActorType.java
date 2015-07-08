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
 * Actor type class for pCG expressions.
 *
 * David Benn, June-August 2000
 */

package cgp.runtime;

import cgp.runtime.ActorException;
import cgp.runtime.ConceptType;
import cgp.runtime.FormalParameter;
import cgp.runtime.FunctionType;
import cgp.runtime.GraphType;
import cgp.runtime.LambdaType;
import cgp.runtime.ScopeStack;
import cgp.runtime.Type;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;

import notio.Actor;
import notio.Concept;
import notio.CopyingScheme;
import notio.Graph;
import notio.Relation;

public class ActorType extends LambdaType {
    // Static fields.
    static private String SELF_NAME = "_self_";
    static private String ANON_NAME = "anonymous";

    // Instance fields.
    private ScopeStack scopes;
    private LinkedList runList;

    // Constructors.

    /** 
     * Invoke this constructor when defining a named actor via an "actor"
     * type definition.
     */
    public ActorType(String name, FormalParameter[] formals, GraphType body) {
	super(name, formals, body);
	this.scopes = Type.getScopeStack();
	this.runList = null;
	setType("actor");
    }

    /** 
     * Invoke this constructor when defining an anonymous actor, typically
     * for direct activation of a graph (e.g. in a process).
     */
    public ActorType(GraphType body) {
	super(ANON_NAME, new FormalParameter[0], body);
	this.scopes = Type.getScopeStack();
	this.runList = null;
	setType("actor");
    }

    /** 
     * Invoke this constructor when copy()ing.
     */
    public ActorType(String name, FormalParameter[] formals,
		     GraphType defGraph, Graph body) {
	super(name, formals, defGraph, body);
	this.scopes = Type.getScopeStack();
	this.runList = null;
	setType("actor");
    }

    // Public methods.

    /**
     * Is this an anonymous actor? This will be the case when the
     * constructor whose signature is ActorType(GraphType) is used.
     */
    public boolean isAnonymous() {
	return getId().equals(ANON_NAME);
    }
    
    /** 
     * Mutate source concept designators with actuals, by argument order.
     * This could be used when there is no actor type definition, and 
     * source concept designators are already bound, so name to actual 
     * argument mapping is impossible, e.g. in the case of a recursive,
     * anonymous actor. Indeed, this would seem to be the *only* case. 
     * It is assumed that a copy of the defining graph is being used when 
     * binding.
     */
    public void bindParametersToSourceConcepts(Type[] actuals) {
	ConceptType[] sources = getSources(body);
	for (int i=0;i<sources.length;i++) {
	    sources[i].setDesignatorValue(actuals[i]);
	}
    }

    /**
     * 1. Identify source, intermediate, and sink concepts. Also look for
     *    conflicting concepts and raise an exception if found.<p>
     * 
     * 2. Check that each formal parameter corresponds to >= 1 designators
     *    of that name.<p>
     * 
     * 3. Determine whether sub-actors exist in this graph, each of which
     *    must correspond to a user-defined function or actor.<p>
     *
     * Note: only does 3. currently.
     */
    public void studyGraph() {
	// 3. Determine whether sub-actors exist in this graph, each of which
	//    must correspond to a user-defined function or actor. Note that
	//    the special sub-actor name SELF_NAME refers to the current actor.
	Actor[] actors = body.getValue().getActorRelations();

	if (actors.length == 0) {
	    String msg = "'" + getId() + "' is not an actor.";
	    throw new ActorException(msg);
	}
	
	for (int i=0;i<actors.length;i++) {
	    Actor actor = actors[i];
	    String name = actor.getType().getLabel();
	    if (!name.equals(SELF_NAME)) {
		Type executor = scopes.find(name);
		if (!(executor instanceof FunctionType) &&
		    !(executor instanceof ActorType)) {
		    String msg = "sub-actor '" + name + 
			"' does not resolve to either a function or an actor.";
		    throw new ActorException(msg);
		}
	    }
	}
    }

    /**
     * Copy this actor such that a clone of the defining graph exists
     * for the purpose of actor execution.
     */
    public LambdaType copy() {
	// Only the graph can be mutated, so just clone the defining graph.
	return new ActorType(name, formals, 
			     defGraph,
			     defGraph.getValue().
			         copy(GraphType.COPYING_SCHEME));
    }

    /**
     * Store each sub-actor in a run list.
     */ 
    public void initActorExecution() {
	// Create the run-list, starting with all sub-actors.
	Actor[] subActors = collectSubActors();
	runList = new LinkedList();
	for (int i=0;i<subActors.length;i++) {
	    runList.add(subActors[i]);
	}
    }

    /**
     * Is this actor instance still executable? 
     */
    public boolean isExecutable() {
	return runList != null && runList.size() > 0;
    }

    /**
     * Return the sub-actor (actor or function) which is next ready to
     * to be executed by virtue of having all input concept designators
     * bound to pCG literals. Note that there may be multiple sub-actors
     * in the ready state simultaneously, but the first encountered on the
     * run-list is arbitrarily chosen. A requirement therefore is that
     * sub-actor execution must be referentially transparent. Note that
     * once chosen for execution, a sub-actor becomes inelligible for
     * execution again.
     *
     * Returns null if no sub-actors can be executed.
     *
     * Pre-conditions: 
     *
     *   - studyGraph() has been invoked so we know a given 
     *     sub-actor maps to an underlying function/actor.
     *
     *   - This method should not be invoked unless isExecutable()
     *     returns true.
     */
    public SubActorInfo getNextSubActor() {
	SubActorInfo info = null;
	boolean isReady = false;

	for (int i=0;i<runList.size();i++) {
	    // Check next sub-actor's executable readiness.
	    Actor subActor = (Actor)runList.get(i);
	    Concept[] inputs = subActor.getInputArguments();
	    LinkedList inArgs = new LinkedList();
	    isReady = true;
	    for (int j=0;j<inputs.length;j++) {
		ConceptType inArg = new ConceptType(inputs[j]);
		if (inArg.hasVarDesignator()) {
		    // This sub-actor isn't ready to execute since
		    // an argument is unbound.
		    isReady = false;
		    break;
		} else {
		    // Accumulate input argument concepts.
		    //
		    // Note that the order of these is dictated by the
		    // input arc ordering. In LF or CGIF, this ordering
		    // can be stated explicitly, and tools such as 
		    // CharGer permit such arc numbering.
		    inArgs.add(inArg);
		}
	    }

	    // If the sub-actor is ready to execute, get the output concepts,
	    // get the underlying function/actor, and wrap these along with
	    // the input arguments in an object. Also remove the sub-actor
	    // from the run-list since it has been cleared for execution
	    // and once run, will not be elligible to do so again.
	    if (isReady) {
		Concept[] outputs = subActor.getOutputArguments();
		LinkedList outArgs = new LinkedList();
		for (int j=0;j<outputs.length;j++) {
		    outArgs.add(new ConceptType(outputs[j]));
		}
		Type executor = null; // keep javac happy
		String name = subActor.getType().getLabel();
		if (!name.equals(SELF_NAME)) {
		    executor = scopes.find(name);
		} else {
		    executor = this;
		}
		info = new SubActorInfo(executor, inArgs, outArgs, true);
		Actor s = (Actor)runList.get(i);
		runList.remove(i);
		break;
	    }
	}

	return info;
    }

    /**
     * Return a hash containing the per sub-actor count of input and output
     * concepts in the supplied graph. These counts can then be used to 
     * determine sink, source, intermediate, or conflicting concept nodes.
     * See [Lukose & Mineau 1998] or my thesis for a description of each.
     */
    static private Hashtable getSubActorIOCounts(GraphType body) {
	Actor[] actors = body.getValue().getActorRelations();
	Concept[] concepts = body.getValue().getConcepts();
	Hashtable ioCounts = new Hashtable();

	// Initialise all concept I/O counts to zero.
	// Any concept which isn't related to an actor
	// will in the end still have zero counts. 
	for (int i=0;i<concepts.length;i++) {
	    ioCounts.put(concepts[i], new ConceptCounts());
	}

	// How many actors does a particular concept input to or output from?
	// There's no need to check whether a "counts" object is null, since
	// all the graph's concepts are in the hash table, as per above loop.
	for (int i=0;i<actors.length;i++) {
	    Concept[] ins = actors[i].getInputArguments();
	    for (int j=0;j<ins.length;j++) {
		ConceptCounts counts = (ConceptCounts)ioCounts.get(ins[j]);
		counts.inCount++;
	    }

	    Concept[] outs = actors[i].getOutputArguments();
	    for (int j=0;j<outs.length;j++) {
		ConceptCounts counts = (ConceptCounts)ioCounts.get(outs[j]);
		counts.outCount++;
	    }
	}

	return ioCounts;
    }


    /**
     * The sink arguments of an actor graph are those which take output
     * from one (unless conflicting, then > 1) sub-actor and provide input
     * to no sub-actor. Note that we are interested in the final outputs 
     * from the actor, not intermediate outputs.
     */
    static public ConceptType[] getSinks(GraphType body) {
	Hashtable ioCounts = getSubActorIOCounts(body);

	// Accumulate the list of sink concepts. Note that the test for 
	// the number of actors which provide output to a particular
	// concept caters for an actor with conflicting sinks, i.e.
	// where multiple sub-actors output to that concept.
	LinkedList sinkList = new LinkedList();
	Enumeration e = ioCounts.keys();
	while (e.hasMoreElements()) {
	    Concept concept = (Concept)e.nextElement();
	    ConceptCounts counts = (ConceptCounts)ioCounts.get(concept);
	    if (counts.inCount == 0 && counts.outCount >= 1) {
		sinkList.add(new ConceptType(concept));
	    }
	}

	return (ConceptType[])sinkList.toArray(new ConceptType[0]);
    }

    /**
     * The source arguments of an actor graph are those which provide input
     * into one or more sub-actors but are outputs of no sub-actor.
     */
    static public ConceptType[] getSources(GraphType body) {
	Hashtable ioCounts = getSubActorIOCounts(body);

	// Accumulate the list of sink concepts. Note that the test for 
	// the number of actors which provide output to a particular
	// concept caters for an actor with conflicting sources, i.e.
	// where multiple sub-actors output to that concept.
	LinkedList sourceList = new LinkedList();
	Enumeration e = ioCounts.keys();
	while (e.hasMoreElements()) {
	    Concept concept = (Concept)e.nextElement();
	    ConceptCounts counts = (ConceptCounts)ioCounts.get(concept);
	    if (counts.inCount >= 1 && counts.outCount == 0) {
		sourceList.add(new ConceptType(concept));
	    }
	}

	return (ConceptType[])sourceList.toArray(new ConceptType[0]);
    }

    // Private methods.

    /** Collect sub-actors in this actor. 
     *
     *  Preconditions:
     *
     *    - The graph has already been studied via studyGraph()
     *      and found to contain valid sub-actors.  
     */
    private Actor[] collectSubActors() {
	return body.getValue().getActorRelations();
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */    

    public ListType getSinkconcepts() {
	return new ListType(ActorType.getSinks(defGraph.nocomments()));
    }

    public ListType getSourceconcepts() {
	return new ListType(ActorType.getSources(defGraph.nocomments()));
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */
}

class ConceptCounts {
    public int inCount = 0;
    public int outCount = 0;
}
