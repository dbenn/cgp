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
 * Function type class for pCG expressions.
 *
 * David Benn, June-October 2000
 */

package cgp.runtime;

import antlr.collections.AST;
import cgp.runtime.FormalParameter;
import cgp.runtime.Scope;
import cgp.runtime.ScopeStack;

public class FunctionType extends Type {
    // Static fields.
    static private String ANON_NAME = "anonymous";

    // Instance fields.
    private String name;
    private FormalParameter[] formals;
    private AST code;
    private Scope env;

    // Constructors.
    public FunctionType(String name, FormalParameter[] formals, AST code) {
	this.name = name;
	this.formals = formals;
	this.code = code;
	env = null; // no extra environment for named functions; not closures
	setType("function");
    }

    public FunctionType(FormalParameter[] formals, AST code) {
	this.name = ANON_NAME;
	this.formals = formals;
	this.code = code;
	// Capture the environment for anonymous functions 
	// to permit closures. This rolled-up environment
	// must be pushed before this function is executed, 
	// and popped once it has ended.
	ScopeStack scopeStack = Type.getScopeStack();
	env = new Scope(); 
	// Coalesce from oldest to newest frame so that
	// the correct name "shadowing" occurs.
	for (int i=0;i<scopeStack.depth();i++) {
	    Scope scope = scopeStack.getFrame(i);
	    String[] names = scope.getNames();
	    for (int j=0;j<names.length;j++) {
		env.def(names[j], scope.get(names[j]));
	    }
	}
	setType("function");
    }

    // Public methods.
 
   public String getId() {
	return name;
    }

    public FormalParameter[] getFormals() {
	return formals;
    }

    public AST getCode() {
	return code;
    }

    public Scope getEnv() {
	return env;
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

    public NumberType getArgcount() {
	return new NumberType(formals.length);
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */
}
