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
 * Process type class for pCG expressions.
 *
 * David Benn, August 2000
 */

package cgp.runtime;

import antlr.collections.AST;

import cgp.runtime.FormalParameter;
import cgp.runtime.Rule;

import java.util.LinkedList;

public class ProcessType extends Type {
    // Fields.
    private String name;
    private FormalParameter[] formals;
    private AST initialBlock;
    private Rule[] rules;

    // Constructors.
    public ProcessType(String name, FormalParameter[] formals, 
		       AST initialBlock, Rule[] rules) {
	this.name = name;
	this.formals = formals;
	this.initialBlock = initialBlock;
	this.rules = rules;
	setType("process");
    }

    // Public methods.

    public String getId() {
	return name;
    }

    public FormalParameter[] getFormals() {
	return formals;
    }

    public AST getInitialBlock() {
	return initialBlock;
    }

    public Rule[] getRules() {
	return rules;
    }

    public String toString() {
	return getType() + " " + name + "; arity " + formals.length;
    }    

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public StringType getName() {
	return new StringType(name);
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */
}
