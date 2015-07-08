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
 * Numeric quantifier.
 *
 * David Benn, June 2001
 */

package cgp.translators;

import notio.QuantifierMacro;

/**
 * A class which represents a numeric concept referent quantifier, for
 * example: @3.
 */
public class NumericQuantifier implements QuantifierMacro {
    // Instance fields.
    int number;

    // Constructors.
    NumericQuantifier(int num) {
	number = num;
    }

    // Interface methods.

    /**
     * Get quantifier name.
     *
     * @return  quantification number as a string 
     */
    public String getName() {
	return number + "";
    }

    /**
     * Execute macro.
     *
     * @param  macro arguments; ignored, so e.g. pass null
     * @return  quantification number in a one element array
     */
    public Object[] executeMacro(Object[] args) {
	Object[] result = new Object[1];
	result[0] = new Integer(number);
	return result;
    }
}
