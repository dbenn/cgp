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
 * A stack of scopes for keeping track of the execution of functions, actors
 * and blocks (in if, while, foreach, action etc blocks).
 *
 * David Benn, July-October 2000.
 */

package cgp.runtime;

import cgp.runtime.Scope;
import cgp.runtime.Type;
import java.util.LinkedList;

public class ScopeStack {
    private LinkedList stack;

    public ScopeStack() {
	stack = new LinkedList();
    }

    public int depth() {
	return stack.size();
    }

    public Scope push(Scope s) {
	stack.addLast(s);
	return s;
    }

    public Scope pop() {
	return (Scope)stack.removeLast();
    }

    public Scope peek() {
	return (Scope)stack.getLast();
    }

    /**
     * Return the ith frame.
     *
     * Precondition: i is >= 0 or < stack depth.
     */
    public Scope getFrame(int i) {
	return (Scope)stack.get(i);
    }

    /**
     * Search this stack of scopes in FILO order for the specified name.
     */
    public Type find(String name) {
	Type value = UndefinedType.undefined;
	for (int i=stack.size()-1;i>=0;i--) {
	    value = ((Scope)stack.get(i)).get(name);
	    if (!(value instanceof UndefinedType)) break;
	}
	return value;
    }
}
