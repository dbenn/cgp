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
 * A stack of scopes for keeping track of defined variable to concept mappings
 * within a CG context. Each context has a corresponding scope or namespace.
 *
 * David Benn, June 2001.
 */

package cgp.translators;

import cgp.translators.ContextScope;
import java.util.LinkedList;
import notio.Concept;

public class ContextScopeStack {
    private LinkedList stack;

    public ContextScopeStack() {
	stack = new LinkedList();
    }

    public int depth() {
	return stack.size();
    }

    public ContextScope push(ContextScope s) {
	stack.addLast(s);
	return s;
    }

    public ContextScope pop() {
	return (ContextScope)stack.removeLast();
    }

    public ContextScope peek() {
	return (ContextScope)stack.getLast();
    }

    /**
     * Return the ith frame.
     *
     * Precondition: i is >= 0 or < stack depth.
     */
    public ContextScope getFrame(int i) {
	return (ContextScope)stack.get(i);
    }

    /**
     * Search this stack of scopes in FILO order for the specified name.
     */
    public Concept find(String name) {
	Concept value = null;
	for (int i=stack.size()-1;i>=0;i--) {
	    value = ((ContextScope)stack.get(i)).get(name);
	    if (value != null) break;
	}
	return value;
    }
}
