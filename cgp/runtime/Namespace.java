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
 * A namespace.
 *
 * David Benn, May-October 2000.
 */

package cgp.runtime;

import cgp.runtime.Type;
import java.util.HashMap;
import java.util.Iterator;

public class Namespace {
    private HashMap table;

    public Namespace() {
	table = new HashMap();
    }

    public Namespace(HashMap map) {
	table = map;
    }

    public void def(String name, Type value) {
	table.put(name, value);
    }

    public Type get(String name) {
	Type value = (Type)table.get(name);
	if (value == null) value = UndefinedType.undefined;
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
    public Namespace copy() {
	HashMap newTable = new HashMap();
	Iterator it = table.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    newTable.put(name, table.get(name));
	}
	return new Namespace(newTable);
    }

    /**
     * Return an array of coreferent variable mappings as pCG strings.
     */
    public StringType[] getMappings() {
	int i = 0;
	StringType[] mappings = new StringType[table.size()];
	Iterator it = table.keySet().iterator();
	while (it.hasNext()) {
	    String name = (String)it.next();
	    mappings[i++] = new StringType(name + " = " + table.get(name));
	}
	return mappings;
    }
}
