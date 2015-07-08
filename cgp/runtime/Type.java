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
 * Type class for pCG expressions.
 *
 * All of pCG's operators (e.g. or, +) have method counterparts,
 * the default behaviour of which is to throw an illegal argument 
 * exception. Each subclass should override these methods where 
 * appropriate. The value of the object on which the operator
 * method is invoked is used as the first operand, or in the
 * case of unary operations, the sole operand.
 *
 * This class can never be instantiated.
 *
 * David Benn, June-August 2000
 */

package cgp.runtime;

import cgp.runtime.StringType;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

abstract public class Type {
    // Instance fields.
    private String type = ""; // subclasses must set this

    // Constructors.
    // Protected since a Type instance can't be created.
    protected Type() {
    }

    // Protected methods.
    protected void setType(String type) {
	this.type = type;
    }

    protected void operationError(String msg) {
	throw new IllegalArgumentException(msg);
    }

    // Public methods.
    // Note: Those methods ending in "Op" are pCG operators.

    public Type orOp(Type other) {
	if (true) { // otherwise javac complains that return is never reached
	    operationError("logical or");
	}
	return null;
    }

    public Type andOp(Type other) {
	if (true) {
	    operationError("logical and");
	}
	return null;
    }

    public Type gtOp(Type other) {
	if (true) {
	    operationError("greater than");
	}
	return null;
    }

    public Type ltOp(Type other) {
	if (true) {
	    operationError("less than");
	}
	return null;
    }

    public Type geOp(Type other) {
	if (true) {
	    operationError("greater than or equal");
	}
	return null;
    }

    public Type leOp(Type other) {
	if (true) {
	    operationError("less than or equal");
	}
	return null;
    }

    public Type eqOp(Type other) {
	if (true) {
	    operationError("equivalence");
	}
	return null;
    }

    public Type neOp(Type other) {
	if (true) {
	    operationError("inequality");
	}
	return null;
    }

    /**
     * Is this type of the type specified by the string value other.
     */
    public Type isOp(Type other) {
	if (other instanceof StringType) {
	    return new BooleanType(type.equals(((StringType)other).
					       getValue()));
	} else {
	    operationError("is-type");
	}
	return null;
    }

    /**
     * Concatenation of this value to a string via toString().
     * This handles the case where in the expression x + y, 
     * y is a string and x is any type.
     *
     * One problem with this approach is that it presupposes
     * the existence of the subclass StringType. I guess this 
     * is okay if pCG's type system is viewed hollistically.
     *
     * A numerical subclass would of course override this method
     * to handle arithmetic addition if the second operand (y) is
     * also a numeric type. A list subclass *could* override this method 
     * to handle addition to the end of the list, where the first operand 
     * (x) is a list, and the second operand (y) is any other type.
     */
    public Type addOp(Type other) {
	Type result = null;
	if (other instanceof StringType) {
	    result = new StringType(this + ((StringType)other).getValue());
	} else {
	    operationError("addition");
	}
	return result;
    }

    public Type subtractOp(Type other) {
	if (true) {
	    operationError("subtraction");
	}
	return null;
    }

    public Type multiplyOp(Type other) {
	if (true) {
	    operationError("multiplication");
	}
	return null;
    }

    public Type divideOp(Type other) {
	if (true) {
	    operationError("division");
	}
	return null;
    }

    public Type modulusOp(Type other) {
	if (true) {
	    operationError("modulus");
	}
	return null;
    }

    public Type negateOp() {
	if (true) {
	    operationError("unary negation");
	}
	return null;
    }

    public Type notOp() {
	if (true) {
	    operationError("logical complement");
	}
	return null;
    }

    /** 
     * List access operator (e.g. x = y[n]).
     */
    public Type getNthOp(Type n) {
	if (true) {
	    operationError("array element access");
	}
	return null;
    }

    /** 
     * List assignment operator (e.g. x[2] = 3).
     */
    public void setNthOp(Type n, Type value) {
	if (true) {
	    operationError("array element assignment");
	}
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public StringType getType() {
	return new StringType(type);
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */

    // -----------------------------------------------------------

    /**
     * Attribute access operator (e.g. t = x.type).
     */
    public Type getAttr(String name) {
	Type result = UndefinedType.undefined;
	boolean found = false;
	String errMsg = "attribute [" + name + "] access";

	// Convert attribute name to method name.
	StringBuffer nameBuf = new StringBuffer(name);
	nameBuf.setCharAt(0, Character.toUpperCase(nameBuf.charAt(0)));
	String methodName = "get" + nameBuf.toString();

	// Search for method. Assume no parameters and Type or Type subclass 
	// return type since all values in pCG are of type Type.
	Class theClass = this.getClass();
	Method[] methods = theClass.getMethods();
	for (int i=0;i<methods.length;i++) {
	    if (methodName.equals(methods[i].getName())) {
		try {
		    // Invoke the method, generating a generic error
		    // since we know a priori that attribute setting
		    // is a simple operation over which we have control.
		    result = (Type)methods[i].invoke(this, null);
		    found = true;
		    break;
		} catch(IllegalArgumentException e) {
		    operationError(errMsg);
		} catch(IllegalAccessException e) {
		    operationError(errMsg);
		} catch(InvocationTargetException e) {
		    operationError(errMsg + " (" + 
				   e.getTargetException() + ")");
		}
	    }
	}

	if (!found) {
	    operationError(errMsg);
	}

	return result;
    }

    /**
     * Attribute assignment operator (e.g. t.referent = "foo").
     */
    public void setAttr(String name, Type x) {
	boolean found = false;
	String errMsg = "attribute [" + name + "] access";

	// Convert attribute name to method name.
	StringBuffer nameBuf = new StringBuffer(name);
	nameBuf.setCharAt(0, Character.toUpperCase(nameBuf.charAt(0)));
	String methodName = "set" + nameBuf.toString();

	// Search for method. Assume void return type and single parameter 
	// of type Type since all values in pCG are of type Type.
	Class theClass = this.getClass();
	Method[] methods = theClass.getMethods();
	for (int i=0;i<methods.length;i++) {
	    if (methodName.equals(methods[i].getName())) {
		try {
		    // Invoke the method, generating a generic error
		    // since we know a priori that attribute setting
		    // is a simple operation over which we have control.
		    Object[] argList = new Object[1];
		    argList[0] = x;
		    methods[i].invoke(this, argList);
		    found = true;
		    break;
		} catch(IllegalArgumentException e) {
		    operationError(errMsg);
		} catch(IllegalAccessException e) {
		    operationError(errMsg);
		} catch(InvocationTargetException e) {
		    operationError(errMsg + " (" + 
				   e.getTargetException() + ")");
		}
	    }
	}

	if (!found) {
	    operationError(errMsg);
	}
    }

    /**
     * pCG member function invocation.
     */
    public Type invokeMemberFunc(String methodName, Object[] actuals) {
	Type result = UndefinedType.undefined;
	boolean found = false;
	String errMsg = "member function \"" + methodName + "\"";

	// Search for a matching method signature (name and parameter number).
	// Note that since pCG member functions will generally take parameters
	// of type Type, rather than more specific types, methods with the same
	// name may only be confidently distinguished in terms of parameter 
	// number.
	Class theClass = this.getClass();
	Method[] methods = theClass.getMethods();
	for (int i=0;i<methods.length;i++) {
	    Class[] formals = methods[i].getParameterTypes();
	    if (methodName.equals(methods[i].getName()) &&
		formals.length == actuals.length) {
		try {
		    // Invoke the method, generating a generic error.
		    // If the return or parameter types are incorrect, 
		    // one of four exceptions will be generated.
		    result = (Type)methods[i].invoke(this, actuals);
		    found = true;
		    break;
		} catch(IllegalArgumentException e) {
		    operationError(errMsg + " (reason: " +
				   e.getMessage() + ")");
		} catch(IllegalAccessException e) {
		    operationError(errMsg + " (reason: " +
				   e.getMessage() + ")");
		} catch(InvocationTargetException e) {
		    operationError(errMsg + " (reason: " + 
				   e.getTargetException() + ")");
		} catch(ClassCastException e) {
		    // Return type cast error.
		    operationError(errMsg + " (reason: " +
				   e.getMessage() + ")");
		}
	    }
	}

	if (!found) {
	    operationError(errMsg + " (reason: not found)");
	}

	return result;
    }

    //---------------------------------------------------------------

    // Static fields and methods.
    //
    // Note: the KB and scope setters and getters mean that only one
    // invocation of the pCG interpreter may safely be running on a
    // a single JVM (the likely case), since storing this information
    // statically is not thread safe. 
    
    static private ScopeStack scopeStack;
    static private KnowledgeBaseStack kbStack;

    static public void setScopeStack(ScopeStack stack) {
	scopeStack = stack;
    }

    static public ScopeStack getScopeStack() {
	return scopeStack;
    }

    static public void setKBStack(KnowledgeBaseStack stack) {
	kbStack = stack;
    }

    static public KnowledgeBaseStack getKBStack() {
	return kbStack;
    }
}
