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
 * Boolean type class for pCG expressions.
 *
 * David Benn, June-July 2000
 */

package cgp.runtime;

import cgp.runtime.Type;

public class BooleanType extends Type {
    // Fields.
    private boolean value;

    // Constructors.
    public BooleanType(boolean b) {
	value = b;
	setType("boolean");
    }

    // Public methods.
    public boolean getValue() {
	return value;
    }

    public String toString() {
	return "" + value;
    }    

    public boolean equals(Object other) {
	if (other instanceof BooleanType) {
	    return value == ((BooleanType)other).getValue();
	} else {
	    return false;
	}
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof BooleanType) {
	    result = new BooleanType(value == ((BooleanType)other).getValue());
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public Type neOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof BooleanType) {
	    result = new BooleanType(value != ((BooleanType)other).getValue());
	} else {
	    super.neOp(other);
	}
	return result;
    }

    public Type orOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof BooleanType) {
	    return new BooleanType(value || ((BooleanType)other).getValue());
	} else {
	    super.orOp(other);
	}
	return result;
    }

    public Type andOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof BooleanType) {
	    return new BooleanType(value && ((BooleanType)other).getValue());
	} else {
	    super.andOp(other);
	}
	return result;
    }

    public Type notOp() {
	return new BooleanType(!value);
    }
 }
