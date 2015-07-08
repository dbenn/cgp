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
 * String type class for pCG expressions.
 *
 * David Benn, June-August 2000
 */

package cgp.runtime;

import cgp.runtime.BooleanType;
import cgp.runtime.GraphType;
import cgp.runtime.NumberType;
import cgp.runtime.Type;
import cgp.runtime.UndefinedType;

public class StringType extends Type {
    // Fields.
    private String value;

    // Constructors.
    public StringType(String s) {
	value = s;
	setType("string");
    }

    // Public methods.

    public String getValue() {
	return value;
    }

    public String toString() {
	return value;
    }    

    public boolean equals(Object other) {
	if (other instanceof StringType) {
	    return value.equals(((StringType)other).getValue());
	} else {
	    return false;
	}
    }

    public Type gtOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(value.compareTo(((StringType)other).
						     getValue()) > 0);
	} else {
	    super.gtOp(other);
	}
	return result;
    }

    public Type ltOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(value.compareTo(((StringType)other).
						     getValue()) < 0);
	} else {
	    super.ltOp(other);
	}
	return result;
    }

    public Type geOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(value.compareTo(((StringType)other).
						     getValue()) >= 0);
	} else {
	    super.geOp(other);
	}
	return result;
    }

    public Type leOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(value.compareTo(((StringType)other).
						     getValue()) <= 0);
	} else {
	    super.leOp(other);
	}
	return result;
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(value.equals(((StringType)other).
						  getValue()));
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public Type neOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof StringType) {
 	    result = new BooleanType(!value.equals(((StringType)other).
						  getValue()));
	} else {
	    super.neOp(other);
	}
	return result;
    }

    /**
     * Concatenation of this string to any other object to form 
     * a new string. This handles the case where in the expression 
     * x + y, x is a string and y is any type.
     */
    public Type addOp(Type other) {
	return new StringType(value + other);
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for pCG attributes.
     */
    public NumberType getLength() {
	return new NumberType(value.length());
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */

    /**
     * Returns a substring of this string where start and end are >= 1
     * and <= length of string. When indices are out of bounds, the 
     * returned value is undefined.
     */
    public Type substring(NumberType start, NumberType end) {
	Type result = UndefinedType.undefined;
	int first = (int)start.getValue()-1;
	int last  = (int)end.getValue();

	if (first >= 0 && first <= value.length() &&
	    last >= 0 && last <= value.length()) {
	    result = new StringType(value.substring(first, last));
	}

	return result;
    }

    /**
     * Returns a substring of this string from start to the end of
     * the string, where start is >= 1. When indices are out of bounds, 
     * the returned value is undefined.
     */
    public Type substring(NumberType start) {
	Type result = UndefinedType.undefined;
	int first = (int)start.getValue()-1;

	if (first >= 0 && first <= value.length()) {
	    result = new StringType(value.substring(first, value.length()));
	}

	return result;
    }

    /**
     * Returns the index (>= 1) of the first occurrence of
     * s in this string, or -1 if it is not found.
     */
    public NumberType index(StringType s) {
	int n = value.indexOf(s.getValue());

	if (n != -1) {
	    n += 1;
	}

	return new NumberType(n);
    }

    /**
     * Replace all occurrences of the single-character string
     * s with the single-character string t in this string. Any
     * error leaves the string unchanged.
     */
    public StringType replace(StringType s, StringType t) {
	String oldStr = s.getValue();
	String newStr = t.getValue();

	if (oldStr.length() == 1 && newStr.length() == 1 && 
	    !oldStr.equals(newStr)) {
	    char oldChar = oldStr.charAt(0);
	    char newChar = newStr.charAt(0);
	    value = value.replace(oldChar, newChar);
	}
	
	return this;
    }

    public BooleanType toBoolean() {
	// true (case insensitive) => true; anything else => false
	return new BooleanType(Boolean.valueOf(value).booleanValue());
    }

    public Type toNumber() {
	Type result = UndefinedType.undefined;
	try {
	    result = new NumberType(Double.parseDouble(value));
	} catch (NumberFormatException e) {
	    // do nothing but return undefined
	}
	return result;
    }

    public GraphType toGraph() {
	return new GraphType(value);
    }
}
