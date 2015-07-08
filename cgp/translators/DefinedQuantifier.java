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
 * Defined quantifier.
 *
 * David Benn, June 2001
 */

package cgp.translators;

import notio.QuantifierMacro;

/**
 * A class which represents a named concept referent quantifier with an
 * optional collection. Examples are: @every, @Col{"red", "green", "blue"}.
 */
public class DefinedQuantifier implements QuantifierMacro {
    // Instance fields.
    String name;
    Object[] collection;

    // Constructors.
    DefinedQuantifier(String name) {
	this.name = name;
	this.collection = null;
    }

    DefinedQuantifier(String name, Object[] collection) {
	this.name = name;
	this.collection = collection;
    }

    // Interface methods.

    /**
     * Get quantifier name.
     *
     * @return  quantifier name as a string 
     */
    public String getName() {
	return name;
    }

    /**
     * Execute macro.
     *
     * @param  macro arguments; ignored, so e.g. pass null
     * @return  collection; may be null
     */
    public Object[] executeMacro(Object[] args) {
	return collection;
    }
}
