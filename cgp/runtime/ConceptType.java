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
 * Concept type class for pCG expressions.
 *
 * The KB where a concept instance is created determines what types (the same
 * for all KBs), markers, and graphs are available to it. Whether this always 
 * makes sense remains to be seen.
 * 
 * David Benn, June-August 2000, June 2001
 */

package cgp.runtime;

import cgp.runtime.GraphException;
import cgp.runtime.KBase;
import cgp.runtime.NumberType;
import cgp.runtime.StringType;
import cgp.runtime.Type;
import cgp.runtime.UndefinedType;
import cgp.translators.DefinedQuantifier;
import cgp.translators.NumericQuantifier;

import java.io.StringReader;
import java.io.StringWriter;

import notio.Concept;
import notio.CopyingScheme;
import notio.CoreferenceSet;
import notio.Designator;
import notio.Generator;
import notio.GeneratorException;
import notio.Graph;
import notio.KnowledgeBase;
import notio.LiteralDesignator;
import notio.Macro;
import notio.Marker;
import notio.MarkerDesignator;
import notio.MarkerSet;
import notio.NameDesignator;
import notio.ParserException;
import notio.Referent;
import notio.TranslationContext;
//import notio.translators.LFParser;
import notio.translators.CGIFParser;
import notio.translators.LFGenerator;

public class ConceptType extends Type {
    // Static fields.
    public static CopyingScheme COPYING_SCHEME = 
	new CopyingScheme(CopyingScheme.GR_COPY_DUPLICATE,
			  CopyingScheme.CN_COPY_DUPLICATE,
			  CopyingScheme.RN_COPY_DUPLICATE,
			  CopyingScheme.DG_COPY_DUPLICATE,
			  CopyingScheme.COMM_COPY_ON,
			  null);

    public static CopyingScheme COPYING_SCHEME_NO_COMMENTS = 
	new CopyingScheme(CopyingScheme.GR_COPY_DUPLICATE,
			  CopyingScheme.CN_COPY_DUPLICATE,
			  CopyingScheme.RN_COPY_DUPLICATE,
			  CopyingScheme.DG_COPY_DUPLICATE,
			  CopyingScheme.COMM_COPY_OFF,
			  null);

    // Instance fields.
    private Concept value;
    private KBase kbase; // the KB with which this concept is associated

    // Constructors.
    public ConceptType(Concept c) {
	kbase = Type.getKBStack().peek(); // where am I *now*
	value = c;
	setType("concept");
    }

    public ConceptType(String s) {
	kbase = Type.getKBStack().peek(); // where am I *now*
	value = parseConcept(s);
	setType("concept");
    }

    // Public methods.

    public Concept getValue() {
	return value;
    }

    public KBase getKBase() {
	return kbase;
    }

    public boolean equals(Object other) {
	if (other instanceof ConceptType) {
	    ConceptType otherCT = (ConceptType)other;

	    String thisTypeName = value.getType().getLabel();
	    String otherTypeName = otherCT.getValue().getType().getLabel();
	    if (!thisTypeName.equals(otherTypeName)) {
		// Concept types don't match.
		return false;
	    } 

	    if (this.hasDesignator() && !otherCT.hasDesignator()) {
		// This has a designator, but the other doesn't.
		return false;
	    }

	    if (!this.hasDesignator() && otherCT.hasDesignator()) {
		// This has no designator, but the other does.
		return false;
	    }

	    if (this.hasDesignator() && otherCT.hasDesignator()) {
		Type thisDesignator = this.getDesignatorValue();
		Type otherDesignator = otherCT.getDesignatorValue();

		if (!thisDesignator.equals(otherDesignator)) {
		    // Both have designators but they aren't equivalent.
		    return false;
		}
	    }

	    Type thisQ = this.getQuantifier();
	    Type otherQ = otherCT.getQuantifier();
	    if (!thisQ.equals(otherQ)) {
		// Quantifiers aren't equivalent.
		return false;
	    }

	    Type thisDesc = this.getDescriptor();
	    Type otherDesc = otherCT.getDescriptor();

	    // Different possibilities re: a blank graph as
	    // the descriptor. See also GraphType.equals().
	    // This needs to be sorted out at the source, i.e. 
	    // in the parser.

	    if (thisDesc == UndefinedType.undefined &&
		otherDesc instanceof GraphType &&
		((GraphType)otherDesc).getValue().isBlank()) {
		// Blank.
		return true;
	    }

	    if (otherDesc == UndefinedType.undefined &&
		thisDesc instanceof GraphType &&
		((GraphType)thisDesc).getValue().isBlank()) {
		// Blank.
		return true;
	    }

	    // Covers two undefined or graph values.
	    if (!thisDesc.equals(otherDesc)) {
		// Descriptors (undefined or graphs) aren't equivalent.
		return false;
	    }
	} else {
	    return false;
	}

	// If we get this far unchallenged, the concepts are equivalent.
	return true;
    }

    public Type eqOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof ConceptType) {
	    result = new BooleanType(this.equals(other));
	} else {
	    super.eqOp(other);
	}
	return result;
    }

    public Type neOp(Type other) {
	Type result = UndefinedType.undefined;
	if (other instanceof ConceptType) {
	    result = new BooleanType(!this.equals(other));
	} else {
	    super.neOp(other);
	}
	return result;
    }

    public String toString() {
	StringWriter w = new StringWriter();
	Generator gen = null;
	if (cgp.CGP.LFOpt) {
	    gen = new LFGenerator();
	} else {
	    // CGIF.
	    try {
		gen = (Generator)
		        (Class.forName(cgp.CGP.CGIFGenOpt)).newInstance();
	    } catch (Exception e) {
		String msg = "error creating CGIF generator " + 
		             cgp.CGP.CGIFGenOpt;
		throw new GraphException(msg);
	    }
	}
	KnowledgeBase kb = kbase.getKB();
	TranslationContext tc = new TranslationContext();

	try {
	    gen.initializeGenerator(w, kb, tc);
	    gen.generateConcept(value);
	} catch (Exception e) {
	    throw new GraphException("error generating concept: " + 
				       w.toString().trim());
	}
 
	return w.toString().trim();
    }  

    // Private methods.
    private Concept parseConcept(String s) {
	Concept c = null;
	 

	// Try parsing LF first, and if that fails, CGIF.
	// Notio's LF parsing is somewhat broken, so except
	// for simple graphs, CGIF is more reliable currently.
	// Probably ought to attempt a CGIF parse before LF,
	// since the former is more likely to be used in pCG
	// programs, so this would result in a speed-up.
	try {
	    /*
	    LFParser par = new LFParser();
	    StringReader r = new StringReader(s);
	    KnowledgeBase kb = kbase.getKB();
	    TranslationContext tc = new TranslationContext();
	    par.initializeParser(r, kb, tc);
	    c = par.parseConcept();
	    */
	    throw new ParserException("Ignore LF to avert Win32 JIT error.");
	} catch (ParserException e1) {	
	    try {
		KnowledgeBase kb = kbase.getKB();
		TranslationContext tc = new TranslationContext();
		
		// Let a factory method figure out what to do with
		// the object which is the purported CG source, and
		// create the currently set CGIF parser.
		notio.Parser parser;
		parser = cgp.CGP.createCGParser(cgp.CGP.CGIFParserOpt, 
						s, kb, tc);
		c = parser.parseConcept();
	    } catch (Exception e2) {
		System.out.println(e2);
		throw new GraphException("error parsing concept");
	    }
	}

	return c;
    }

    public boolean hasDesignator() {
	Referent ref = value.getReferent();
	if (ref == null) return false;
	Designator d = ref.getDesignator();
	return d != null;
    }

    public boolean hasVarDesignator() {
	Referent ref = value.getReferent();
	if (ref == null) return false;
	Designator d = ref.getDesignator();
	if (d == null) return false;
	if (d.getDesignatorKind() == Designator.DESIGNATOR_NAME) {
	    String s = ((NameDesignator)d).getName();
	    // TBD: ensure use of both ? and * elsewhere is implemented.
	    return s.startsWith("*") || s.startsWith("?");
	} else {
	    return false;
	}
    }

    public boolean hasBoundDesignator() {
	Referent ref = value.getReferent();
	if (ref == null) return false;
	Designator d = ref.getDesignator();
	if (d == null) return false;
	int kind = d.getDesignatorKind();
	return kind == Designator.DESIGNATOR_LITERAL || 
	       kind == Designator.DESIGNATOR_MARKER ||
	       kind == Designator.DESIGNATOR_NAME;
    }

    /**
     * Convert a Java designator value into one consumable by pCG.
     * TBD: examine carefully how each kind is being used, both here and
     * in setDesignatorValue(), e.g. markers.
     */
    public Type getDesignatorValue() {
	Type result = UndefinedType.undefined;
	Referent ref = value.getReferent();
	if (ref == null) return result;
	Designator d = ref.getDesignator();
	if (d == null) return result;

	switch(d.getDesignatorKind()) {
	    // According to Notio docs, it's unclear what this is used for.
	case Designator.DESIGNATOR_DEFINED: // see dpANS
	    result = UndefinedType.undefined;
	    break;
	    
	    // pCG supports string and number literals, but not integers.
	    // Notice special handling for booleans! Important for actors.
	    // TBD: Note that for this to work, booleans must be double
	    //      quoted, not single quoted, otherwise they'll be name
	    //      designators! Should add code to name designator case
	    //      to handle booleans also, since we shouldn't care what
	    //      quotes are used.
	case Designator.DESIGNATOR_LITERAL:
	    Object literal = ((LiteralDesignator)d).getLiteral();
	    if (literal instanceof Long) {
		result = new NumberType(((Long)literal).doubleValue());
	    } else if (literal instanceof Double) {
		result = new NumberType(((Double)literal).doubleValue());
	    } else if (literal instanceof String) {
		String strLit = (String)literal;
		// Booleans must be double-quoted for this to work!
		// Otherwise, if single-quoted, will be a designator name!
		// Which is the correct representation: literal or name?
		if (strLit.toLowerCase().equals("true")) {
		    result = new BooleanType(true);
		} else if (strLit.toLowerCase().equals("false")) {
		    result = new BooleanType(false);
		} else {
		    result = new StringType(strLit);
		}
	    }
	    break;
	    
	    // Individual marker.
	    // For example: #x or #34
	case Designator.DESIGNATOR_MARKER: // see dpANS
	    result = new StringType((String)((MarkerDesignator)d).
				    getMarker().getIndividual()); 
	                            // getMarker().getMarkerID()); 	    
	    break;
	    
	    // Single-quoted string, e.g. 'foo', '*x'
	    // TBD: Is defined/bound variable a correct use of name 
	    //      designators? Should be able to simply say: *x.
	    //      See also long comment re: this in setDesignatorValue().
	case Designator.DESIGNATOR_NAME: // see dpANS
	    result = new StringType(((NameDesignator)d).getName());
	    break;
	    
	default:
	    result = UndefinedType.undefined; // or throw an exception?
	    break;
	}

	return result;
    }

    /**
     * Convert a pCG designator value into one consumable by Java.
     */
    public void setDesignatorValue(Type newRef) {
	MarkerSet mSet = kbase.getKB().getMarkerSet();

	if (newRef instanceof StringType) {
	    String str = ((StringType)newRef).getValue();
	    if (str.startsWith("#")) {
		// Marker designator.
		MarkerDesignator marker = 
		    new MarkerDesignator(new Marker(mSet, str));
		value.setReferent(new Referent(marker));
		return;
	    } else if (str.startsWith("*")) {
		// Name designator.
		// Note: This is completely at odds with the idea of a 
		//       name designator in the CG Standard!! A name
		//       designator is just a single-quoted string intended
		//       as a locator. I did this before I knew the Standard
		//       so well. Indeed, the whole notion of a name starting 
		//       with *x is just plain wrong. These are meant to be
		//       defined variables, a way of identifying and referring
		//       to a concept elsewhere in a context, i.e. a
		//       coreference variable. In a future revision of pCG,
		//       something like *x should be replaced by something like
		//       $x in the context of an actor etc, i.e. really make
		//       it look like a referent/designator variable that can
		//       be bound to any pCG value. The use of *<ident> in 
		//       Sowa 1984 only adds to the (my) confusion.
	        NameDesignator name = new NameDesignator(str);
		value.setReferent(new Referent(name));
		return;
	    }
	}

	// Assume a numeric, string, or boolean literal designator.
	LiteralDesignator litDes = new LiteralDesignator();
	if (newRef instanceof NumberType) {
	    litDes.setLiteral(new Double(((NumberType)newRef).
					 getValue()), mSet);
	    value.setReferent(new Referent(litDes));
	} else if (newRef instanceof StringType) {
	    litDes.setLiteral(((StringType)newRef).getValue(), mSet);
	    value.setReferent(new Referent(litDes));
	} else if (newRef instanceof BooleanType) {
	    String val = ((BooleanType)newRef).getValue() == true ?
		"true" : "false";
	    litDes.setLiteral(val, mSet);
	    value.setReferent(new Referent(litDes));
	}
    }

    /**
     * Attempt to restrict this concept by type and/or referent
     * according to the supplied concept. A boolean result indicates
     * whether the restriction was successful. Type restriction may
     * only occur if the supplied concept's type is a proper subtype
     * of this concept's type. No attempt is made to restrict by 
     * quantifier, only marker/literal designator and descriptor.
     */
    public boolean restriction(ConceptType otherCT) {
	boolean typeRestriction = false;
	boolean referentRestriction = false;

	// Type restriction?
	notio.ConceptType thisType = value.getType();
	notio.ConceptType otherType = otherCT.getValue().getType();
	if (thisType.hasProperSubType(otherType)) {
	    value.setType(otherType);
	    typeRestriction = true;
	}

	// Referent restriction can only take place if the source and target
	// types are identical. Type restriction may already have taken care
	// of this.
	if (value.getType().getLabel().equals(otherType.getLabel())) {
	    if (value.isGeneric() || this.hasVarDesignator()) {
		boolean wasNameDesignator = this.hasVarDesignator();
		String nameDesignator = null;
		if (wasNameDesignator) {
		    nameDesignator = ((StringType)this.
				      getDesignatorValue()).getValue();
		}

		if (otherCT.hasBoundDesignator()) {
		    // Marker or literal referent restriction from generic or
		    // name designator.
		    this.setDesignatorValue(otherCT.getDesignatorValue());
		    if (wasNameDesignator) {
			kbase.addCorefVarMapping(nameDesignator,
						 otherCT.getDesignatorValue());
		    }
		    referentRestriction = true;
		} else {
		    Type desc = otherCT.getDescriptor();
		    if (desc instanceof GraphType) {
			// Descriptor referent restriction from generic or
			// name designator.
			GraphType descGraph = (GraphType)desc;
			this.setDescriptor(descGraph);
			if (wasNameDesignator) {
			    kbase.addCorefVarMapping(nameDesignator,
						     descGraph);
			}
			referentRestriction = true;
		    }
		}
	    }
	}

	return typeRestriction || referentRestriction;
    }

    // -----------------------------------------------------------

    /**
     * Getter and setter methods for attributes.
     */
    public StringType getLabel() {
	return new StringType(value.getType().getLabel());
    }

    public Type getDesignator() {
	return getDesignatorValue();
    }

    public void setDesignator(Type x) {
	setDesignatorValue(x);
    }

    public Type getDescriptor() {
	Type result = UndefinedType.undefined;
	Referent ref = value.getReferent();

	if (ref != null) {
	    Graph desc = ref.getDescriptor();
	    if (desc != null) {
		result = new GraphType(desc);
	    }
	}

	return result;
    }

    public void setDescriptor(GraphType desc) {
	value.setReferent(new Referent(desc.getValue()));
    }

    public Type getQuantifier() {
	Type result = UndefinedType.undefined;
	Referent ref = value.getReferent();

	if (ref != null) {
	    Macro macro = ref.getQuantifier();
	    if (macro != null) {
		if (macro instanceof NumericQuantifier) {
		    // For example: @3
		    Object[] res = macro.executeMacro(null);
		    int num = ((Integer)res[0]).intValue();
		    return new NumberType((double)num);
		} else if (macro instanceof DefinedQuantifier) {
		    Object[] res = macro.executeMacro(null);
		    // Collection or no?
		    if (res == null) {
			// For example: @every
			result = new StringType(macro.getName());
		    } else {
			// For example: @Col{"red", "green", "blue"}
			StringType[] list = new StringType[res.length];
			for (int i=0;i<res.length;i++) {
			    list[i] = new StringType((String)res[i]);
			}
			result = new ListType(list);
		    }
		} else {
		    // For any other generic macro.
		    result = new StringType(macro.getName());
		}
	    }
	}

	return result;
    }

    // -----------------------------------------------------------

    /**
     * pCG member functions.
     */  

    /**
     * Attempt to restrict this concept by type and/or referent
     * according to the supplied concept.
     */
    public BooleanType restrict(ConceptType otherCT) {
	return new BooleanType(restriction(otherCT));
    }

    /**
     * Make a copy of this concept.
     */
    public ConceptType copy() {
	return new ConceptType(value.copy(COPYING_SCHEME));
    }

    /**
     * Make a copy of this concept without comments.
     */
    public ConceptType nocomments() {
	return new ConceptType(value.copy(COPYING_SCHEME_NO_COMMENTS));
    }

    public BooleanType isGeneric() {
	return new BooleanType(value.isGeneric());
    }

    public BooleanType isContext() {
	boolean whether = false;
	Referent ref = value.getReferent();

	if (ref != null) {
	    whether = ref.isContext();
	}

	return new BooleanType(whether);
    }
}
