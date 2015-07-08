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
 * Number type class for pCG expressions.
 *
 * David Benn, June-October 2000
 */

package cgp.runtime;

import cgp.runtime.BooleanType;
import cgp.runtime.Type;

public class NumberType extends Type {
    // Fields.
    private double value;

    // Constructors.
    public NumberType(double n) {
	value = n;
	setType("number");
    }

    // Public methods.

    public double getValue() {
	return value;
    }

    public String toString() {
	return "" + value;
    }    

    public boolean equals(Object other) {
	if (other instanceof NumberType) {
	    return value == ((NumberType)other).getValue();
	} else {
	    return false;
	}
    }

    public Type gtOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value > ((NumberType)other).getValue());
	} else {
	    super.gtOp(other);
	}
	return result;
    }

    public Type ltOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value < ((NumberType)other).getValue());
	} else {
	    super.ltOp(other);
	}
	return result;
    }

    public Type geOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value >= ((NumberType)other).getValue());
	} else {
	    super.geOp(other);
	}
	return result;
    }

    public Type leOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value <= ((NumberType)other).getValue());
	} else {
	    super.leOp(other);
	}
	return result;
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value == ((NumberType)other).getValue());
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public Type neOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new BooleanType(value != ((NumberType)other).getValue());
	} else {
	    super.neOp(other);
	}
	return result;
    }

    /**
     * Arithmetic addition.
     */
    public Type addOp(Type other) {
	if (other instanceof NumberType) {
	    return new NumberType(value + ((NumberType)other).getValue());
	} else {
	    return super.addOp(other);
	}
    }

    public Type subtractOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new NumberType(value - ((NumberType)other).getValue());
	} else {
	    super.subtractOp(other);
	}
	return result;
    }

    public Type multiplyOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new NumberType(value * ((NumberType)other).getValue());
	} else {
	    super.multiplyOp(other);
	}
	return result;
    }

    public Type divideOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new NumberType(value / ((NumberType)other).getValue());
	} else {
	    super.divideOp(other);
	}
	return result;
    }

    public Type modulusOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof NumberType) {
	    result = new NumberType(value % ((NumberType)other).getValue());
	} else {
	    super.modulusOp(other);
	}
	return result;
    }

    public Type negateOp() {
	return new NumberType(-value);
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for pCG attributes.
     */

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     *
     *     "// **" indicates that this value is mutated.
     */
    public NumberType pow(NumberType exponent) {
	return new NumberType(Math.pow(value, exponent.getValue()));
    }

    public NumberType sqrt() {
	return new NumberType(Math.sqrt(value));
    }

    public NumberType sin() {
	return new NumberType(Math.sin(value));
    }

    public NumberType cos() {
	return new NumberType(Math.cos(value));
    }

    public NumberType tan() {
	return new NumberType(Math.tan(value));
    }

    public NumberType floor() {
	return new NumberType(Math.floor(value));
    }

    public NumberType ceil() {
	return new NumberType(Math.ceil(value));
    }

    public NumberType round() {
	if (value > 0) {
	    return new NumberType(Math.floor(value));
	} else {
	    return new NumberType(Math.ceil(value));
	}
    }

    // **
    public NumberType inc() {
	value++;
	return this;
    }

    // **
    public NumberType dec() {
	value--;
	return this;
    }

    /**
     * Return a string which this number represents as ASCII.
     */
    public StringType chr() {
	byte[] ch = new byte[1];
	ch[0] = (byte)value;
	return new StringType(new String(ch));
    }
}
