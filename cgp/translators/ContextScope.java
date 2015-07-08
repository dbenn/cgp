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
 * A namespace corresponding to a CG context.
 *
 * David Benn, June 2001.
 */

package cgp.translators;

import java.util.HashMap;
import java.util.Iterator;
import notio.Concept;

public class ContextScope {
    private HashMap table;

    public ContextScope() {
	table = new HashMap();
    }

    public ContextScope(HashMap map) {
	table = map;
    }

    public void def(String name, Concept value) {
	table.put(name, value);
    }

    public Concept get(String name) {
	Concept value = (Concept)table.get(name);
	return value;
    }

    /**
     * Return the names in this namespace.
     */
    public String[] getNames() {
	int i = 0;
	String[] names = new String[table.size()];

	Iterator it = table.keySet().iterator();
	while (it.hasNext()) {
	    names[i++] = (String)it.next();
	}
	
	return names;
    }

    /**
     * Copy this namespace. The members themselves are not deep copied
     * just the container. The latter can then be added to without
     * polluting the original.
     */
    public ContextScope copy() {
	HashMap newTable = new HashMap();
	Iterator it = table.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    newTable.put(name, table.get(name));
	}
	return new ContextScope(newTable);
    }
}
