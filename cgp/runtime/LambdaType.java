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
 * Lambda type class for pCG expressions. This permits parameterised graph
 * types as per Sowa 2000. Hmm. Really? See notio.ConceptTypeDefinition
 * and notio.RelationTypeDefinition. What this class represents however is
 * consistent with the description of lambda in the CG draft ANSI standard.
 * The main motivation for this class is as the basis for ActorType.
 *
 * David Benn, June-September 2000
 */

package cgp.runtime;

import cgp.runtime.FormalParameter;
import cgp.runtime.GraphType;
import cgp.runtime.Namespace;
import cgp.runtime.Type;

import notio.Concept;
import notio.Graph;

public class LambdaType extends Type {
    // Fields.
    protected String name;
    protected FormalParameter[] formals;
    protected GraphType defGraph; // preserve for multiple invocations
    protected GraphType body;

    // Constructors.
    public LambdaType(String name, FormalParameter[] formals, GraphType body) {
	this.name = name;
	this.formals = formals;
	this.defGraph = body;
	this.body = body;
	setType("lambda");
    }

    /**
     * Call this constructor from copy().
     */
    protected LambdaType(String name, FormalParameter[] formals,
			 GraphType defGraph, Graph body) {
	this.name = name;
	this.formals = formals;
	this.defGraph = defGraph;
	this.body = new GraphType(body);
	setType("lambda");
    }
 
    // Public methods.

    public String getId() {
	return name;
    }

    public FormalParameter[] getFormals() {
	return formals;
    }

    public GraphType getBody() {
	return body;
    }

    public String toString() {
	return getType() + " " + name + "; arity " + formals.length;
    }    

    public LambdaType copy() {
	// Only the graph can be mutated, so just clone the defining graph.
	return new LambdaType(name, formals,
			      defGraph,
			      defGraph.getValue().
			          copy(GraphType.COPYING_SCHEME));
    }

    public void bindParameters(Type[] actuals) {
	// Map names to values.
	//
	// Note that the correct name to value mapping is
	// dependent upon the actual parameter list ordering.
	// See also ActorType.getNextSubActor().
	Namespace bindings = new Namespace();
	for (int i=0;i<formals.length;i++) {
	    bindings.def("*" + formals[i].getName(), actuals[i]);
	}
	
	// Mutate designator referents with defined variable 
	// names matching formal parameter names. Ensure this
	// operation is being done with a copy of the original
	// LambdaType instance.
	//
	// TBD: Could use ActorType.getSources(body) to narrow
	// the search here! Hmm. This implies that getSinks()
	// and getSources() could be moved to LambdaType from
	// ActorType.
	Concept[] concepts = body.getValue().getConcepts();
	for (int i=0;i<concepts.length;i++) {
	    ConceptType concept = new ConceptType(concepts[i]);
	    if (concept.hasVarDesignator()) {
		Type designator = concept.getDesignatorValue();
		if (designator instanceof StringType) {
		    String name = ((StringType)designator).getValue();
		    Type value = bindings.get(name);
		    if (value != UndefinedType.undefined) {
			concept.setDesignatorValue(value);
		    }
		}
	    }
	}
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public StringType getName() {
	return new StringType(name);
    }

    public GraphType getDefgraph() {
	return body;
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */
}
