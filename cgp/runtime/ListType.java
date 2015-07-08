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
 * List type class for pCG expressions.
 *
 * David Benn, June-July 2000
 */

package cgp.runtime;

import cgp.runtime.BooleanType;
import cgp.runtime.Type;
import java.util.Iterator;
import java.util.LinkedList;

public class ListType extends Type {
    // Fields.
    private LinkedList theList;

    // Constructors.
    public ListType() {
	theList = new LinkedList();
	setType("list");
    }

    public ListType(LinkedList list) {
	this();
	theList = list;
    }

    public ListType(Type[] values) {
	this();
	for (int i=0;i<values.length;i++) {
	    theList.add(values[i]);
	}
    }

    // Public methods.

    public LinkedList getValue() {
	return theList;
    }

    public String toString() {
	String listStr = "";
	if (theList.size() > 0) {
	    Iterator it = theList.iterator();
	    listStr = "{";
	    while (it.hasNext()) {
		listStr += it.next();
		if (it.hasNext()) listStr += ", ";
	    }	
	    listStr += "}";
	} else {
	    listStr = "{}";
	}
	return listStr;
    }    

    public boolean equals(Object other) {
	if (other instanceof ListType) {
	    LinkedList l = ((ListType)other).getValue();
	    if (l.size() == theList.size()) {
		Iterator it1 = theList.iterator();
		Iterator it2 = l.iterator();
		while (it1.hasNext()) {
		    Type x = (Type)it1.next();
		    Type y = (Type)it2.next();
		    if (!x.equals(y)) return false;
		}	    
	    } else {
		return false;
	    }
	} else {
	    return false;
	}
	return true;
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof ListType) {
	    result = new BooleanType(this.equals(other));
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public Type neOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof ListType) {
	    result = new BooleanType(!this.equals(other));
	} else {
	    super.neOp(other);
	}
	return result;
    }

    /**
     * Return the Nth element of this list. 
     * The first valid index is 1 and the last is the list's length.
     */
    public Type getNthOp(Type n) {
	if (n instanceof NumberType) {
	    int index = (int)((NumberType)n).getValue() - 1;
	    if (index >= 0 && index < theList.size()) {
		return (Type)theList.get(index);
	    } else {		
		throw new IndexOutOfBoundsException("invalid list index: "+n);
	    }
	} else {
	    super.getNthOp(n);
	}
	return UndefinedType.undefined; // to keep javac happy
    }

    /**
     * Set the Nth element of this list.
     * The first valid index is 1 and the last is the list's length.
     */
    public void setNthOp(Type n, Type value) {
	if (n instanceof NumberType) {
	    int index = (int)((NumberType)n).getValue() - 1;
	    if (index >= 0 && index < theList.size()) {
		theList.set(index, value);
	    } else {		
		throw new IndexOutOfBoundsException("invalid list index: "+n);
	    }
	} else {
	    super.setNthOp(n, value);
	}
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public NumberType getLength() {
	return new NumberType(theList.size());
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */

    /** 
     * Is x a member of this list?
     * Note that this is a shallow test.
     */
    public BooleanType hasMember(Type x) {
	boolean found = false;
	Iterator it = theList.iterator();
	while (it.hasNext()) {
	    Type y = (Type)it.next();
	    found = x.equals(y);
	    if (found) break;
	}
	return new BooleanType(found);
    }

    /** 
     * Is x a member of this list, or of a sub-list of this list?
     * If so, return the sub-list within which it is embedded.
     * Note that this may be the outermost list.
     */
    public Type member(Type x) {
	Iterator it = theList.iterator();

	while (it.hasNext()) {
	    Type y = (Type)it.next();
	    if (x.equals(y)) {
		// x and y are structurally equivalent.
		return this;
	    } else if (y instanceof ListType) {
		// Is x a member of the sub-list y?
		// Only return if the value is not undefined!
		Type value = ((ListType)y).member(x);
		if (value instanceof ListType) {
		    return value;
		}
	    }
	}
	
	// Nothing in this list or one of its sub-lists matches x. 
	return UndefinedType.undefined;
    }

    /**
     * Prepend a value to this list and return the mutated list.
     */
    public ListType prepend(Type x) {
	theList.addFirst(x);
	return this;
    }

    /**
     * Append a value to this list and return the mutated list.
     */
    public ListType append(Type x) {
	theList.addLast(x);
	return this;
    }

    /**
     * Merge another list with this list, appending the former's
     * members to the latter's, and returning the mutated list.
     */
    public ListType merge(Type x) {
	if (x instanceof ListType) {
	    theList.addAll(((ListType)x).getValue());
	    return this;
	} else {
	    operationError("merge");
	}
	return this; // keep javac happy
    }
}
