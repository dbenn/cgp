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
 * Undefined type class for pCG expressions.
 *
 * The motivations for this class are:
 *
 *   - A function exits with no return statement, so the value of the
 *     function is undefined. 
 *
 * David Benn, June-July 2000
 */

package cgp.runtime;

import cgp.runtime.Type;

public class UndefinedType extends Type {
    // Fields.
    public static UndefinedType undefined = new UndefinedType();

    // Constructors.
    public UndefinedType() {
	setType("undefined");
    }

    // Public methods.

    public String toString() {
	return getType().getValue();
    }    

    public boolean equals(Object other) {
	return other instanceof UndefinedType;
    }
}
