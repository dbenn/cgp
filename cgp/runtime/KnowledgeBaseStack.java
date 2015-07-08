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
 * A stack of knowledge bases for intrinsic pCG graph operations at the 
 * top-level and in processes. Knowledge bases are also used for parsing
 * and generation of concepts and graphs.
 *
 * David Benn, July-August 2000.
 */

package cgp.runtime;

import cgp.runtime.KBase;
import cgp.runtime.Type;

import java.util.LinkedList;

public class KnowledgeBaseStack {
    // Instance fields.
    private LinkedList stack;

    // Constructors.
    public KnowledgeBaseStack() {
	stack = new LinkedList();
    }

    // Public methods.
    public int depth() {
	return stack.size();
    }

    public KBase push(KBase k) {
	stack.addLast(k);
	return k;
    }

    public KBase pop() {
	return (KBase)stack.removeLast();
    }

    public KBase peek() {
	return (KBase)stack.getLast();
    }
}
