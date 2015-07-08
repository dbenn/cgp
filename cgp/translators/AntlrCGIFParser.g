header {
    package cgp.translators;

	import antlr.SemanticException;

	import cgp.translators.ContextScope;
	import cgp.translators.ContextScopeStack;
	import cgp.translators.DefinedQuantifier;
	import cgp.translators.NumericQuantifier;

	import notio.Actor;
	import notio.Concept;
	import notio.ConceptAddError;
	import notio.ConceptReplaceException;
	import notio.ConceptType;
	import notio.ConceptTypeHierarchy;
	import notio.CopyingScheme;
	import notio.Designator;
	import notio.Graph;
	import notio.KnowledgeBase;
	import notio.LiteralDesignator;
	import notio.Macro;
	import notio.Marker;
	import notio.MarkerDesignator;
	import notio.MarkerSet;
	import notio.NameDesignator;
	import notio.Parser;
	import notio.QuantifierMacro;
	import notio.Referent;
	import notio.Relation;
	import notio.RelationAddError;
	import notio.RelationType;
	import notio.RelationTypeHierarchy;

	import java.util.ArrayList;
}

/**
 * A Notio CGIF parser for a conceptual graph language which embodies Guy
 * Mineau's process formalism.
 * Copyright (C) 2001 David Benn
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
 * Note that this parser is based upon the modified June 2001 CG Standard 
 * found at: http://www.cs.nmsu.edu/~hdp/CGTools/cgstand/cgstandnmsu.html
 * This will be referred to as "The Standard" or "The CGIF Standard" herein. 
 *
 * All parser and lexer productions are preceded by the text from the 
 * aforementioned URL given for each production or its equivalent, although
 * the ordering may not be as found in that document depending upon what
 * makes sense for the ANTLR-based grammar. Notes and questions regarding
 * the original productions have been added.
 *
 * David Benn, June 2001
 */

class AntlrCGIFParser extends Parser;

options {
    exportVocab=AntlrCGIF;
	buildAST=true; // for diagnostics
    defaultErrorHandler=false;
}

{
	// Extra instance fields.
    private CopyingScheme COPYING_SCHEME;
	private KnowledgeBase kBase;
	private ConceptTypeHierarchy conceptTypes;
	private RelationTypeHierarchy relationTypes;
	private ContextScopeStack contexts;
	
	// Constructor: also takes a KB.
	public AntlrCGIFParser(AntlrCGIFLexer lexer, KnowledgeBase kBase) {
		this(lexer);
		this.kBase = kBase;

		this.conceptTypes = kBase.getConceptTypeHierarchy();
		this.relationTypes = kBase.getRelationTypeHierarchy();
		
		COPYING_SCHEME = 
			new CopyingScheme(CopyingScheme.GR_COPY_DUPLICATE,
							  CopyingScheme.CN_COPY_DUPLICATE,
						      CopyingScheme.RN_COPY_DUPLICATE,
						      CopyingScheme.DG_COPY_DUPLICATE,
						      CopyingScheme.COMM_COPY_ON,
						      null);

		contexts = new ContextScopeStack();
		contexts.push(new ContextScope()); // top-level context scope
	}
}

// ---------------------------------------------------------------------

// Actor.
// An actor begins with "<" followed by a type. It continues with zero or
// more input arcs, a separator "|", zero or more output arcs, and an optional
// comment. It ends with ">". 
//
// The arcs that precede the vertical bar are called input arcs, and the 
// arcs that follow the vertical bar are called output arcs. The valence N
// of the actor type must be equal to the sum of the number of input arcs
// and the number of output arcs. 		
actor returns [Actor a]
{
	a = null;
	String label = null;
	ArrayList inputs = new ArrayList();
	ArrayList outputs = new ArrayList();
	Concept c = null;
}			
			:	LANGLE
				label = type //[valence] 

				( c = arc { inputs.add(c); } )*
				VERTBAR
				( c = arc { outputs.add(c); } )*

				(COMMENT)? // can't store this in notio.Actor, so ignore?

				RANGLE
				{
					Concept[] in = (Concept[])inputs.toArray(new Concept[0]);
					Concept[] out = (Concept[])outputs.toArray(new Concept[0]);
					int valence = in.length + out.length;
					RelationType rt = relationTypes.getTypeByLabel(label);
					if (rt == null) {
						rt = new RelationType(label, valence);
						relationTypes.addTypeToHierarchy(rt);
					}
					a = new Actor(rt, in, out);
				}
		    ;

// Arc.
// An arc is a concept or a bound label. 

arc returns [Concept c]
{
	c = null;
}
			:	c = concept
			|   c = boundLabel
			;

// BoundLabel.
// A bound label is a question mark "?" followed by an identifier. 

boundLabel returns [Concept c]
{
	c = null;
}
			   :  QUESTION label:IDENTIFIER
			   {
			       String id = #label.getText();
				   c = contexts.find(id);				  
				   if (c == null) {
					   throw new SemanticException("bound label '?"+id +
												   "' not found.");
				   }
			   }
			;

// CGStream. 
// A conceptual graph stream is defined as a sequence of one or more CGs,
// each separated by a period. 
//
// Since a CG may itself be empty, the string "....." would also qualify as a 
// CG Stream; as well as an empty file. 
//
// Note: Don't bother to add blank graphs since the blank graph always exists.
//       Is this really desirable? The presence of a blank graph may also 
//       indicate that a duplicate relation was found.
//
// An entry point to the parser.

cgStream returns [Graph[] stream]
{
	ArrayList list = new ArrayList();
	Graph g = null;
	stream = null;
} 			
			:	g = cg { if (!g.isBlank()) list.add(g); }
				( DOT! g = cg  { if (!g.isBlank()) list.add(g); } )*
				{ stream = (Graph[])list.toArray(new Graph[0]); }
	        ;

// CG.
// A conceptual graph is a list of zero or more concepts, conceptual
// relations, actors, special contexts, or comments. 
//
// The alternatives may occur in any order provided that any bound
// coreference label must occur later in the CGIF stream and must be within
// the scope of the defining label that has an identical identifier. The
// definition permits an empty CG, which contains nothing. An empty CG, which 
// says nothing, is always true.
//
// An entry point to the parser.

cg returns [Graph g]
{
	g = new Graph();
	Actor a = null;
	Concept c = null;
	Object o = null;
	Relation r = null;
} 		
				: ( options { warnWhenFollowAmbig=false; } :
				(NEGATION_PREFIX) => specialContext[g]

				| (LBRACK specialConLabel) => specialContext[g]

				| c = concept
				  {
					try {
				      g.addConcept(c);
					} catch (ConceptAddError e) {
						// Ignore for now. This was observed in the
						// context of examples/CGTools01/basic-info.cgp
						// when applied to Sowa's testmin.cgf CG stream.
					    // When a call to g.replaceGraph() was attempted
						// here, a stack overflow occurred, due to a deep 
						// exception stacking. In the case in question, 2
						// identical relations were being parsed from the
						// stream, so the upshot of ignoring this error in
						// that case is just that we end up with one relation
						// in the final output list, rather than 2, a 
						// desirable state of affairs. What worries me is
						// the cases I haven't seen that wouldn't turn out
						// so well, whatever they might be.
					}
				  }

			    | r = relation
				  { 
					try {
						g.addRelation(r); 
					} catch(ConceptAddError e) {
						// Notio says we have a reference to the same
						// concept in two different graphs, so copy all
					    // arguments just to be sure. Only happens when
						// adding a relation/actor to a graph not during
						// construction of the relation/actor.
						Concept[] args = r.getArguments();
						for (int i=0;i<args.length;i++) {
							r.replaceArgument(args[i],
											  args[i].copy(COPYING_SCHEME));
						}
						g.addRelation(r);
					} catch (RelationAddError e) {
				        // Should never happen because we new all relations
					    // unlike concepts which are sometimes taken from
					    // a symbol table via bound labels.
				    }
				  }

				| a = actor
				  {
					try {
						g.addRelation(a); 
					} catch(ConceptAddError e) {
						// Notio says we have a reference to the same
						// concept in two different graphs, so copy all
					    // arguments just to be sure. Only happens when
						// adding a relation/actor to a graph not during
						// construction of the relation/actor.
						Concept[] args = r.getArguments();
						for (int i=0;i<args.length;i++) {
							r.replaceArgument(args[i],
											  args[i].copy(COPYING_SCHEME));
						}
						g.addRelation(a);
					} catch (RelationAddError e) {
				        // Should never happen because we new all relations
					    // unlike concepts which are sometimes taken from
					    // a symbol table via bound labels.
				    }
				  }

				| str:COMMENT { g.addComment(#str.getText()); } )*
 	 	    ;

// Concept.
// A concept begins with a left bracket "[" and an optional monadic type
// followed by optional coreference links and an optional referent in either
// order. It ends with an optional comment and a required "]". 
//
// If the type is omitted, the default type is Entity. This rule permits the
// coreference labels to come before or after the referent. If the referent
// is a CG that contains bound labels that match a defining label on the
// current concept, the defining label must precede the referent. 
//
// In Figure 4, for example, the concept [Person: Mary] could be written in
// CGIF as [Person:'Mary'*x]; the coreferent concept [&top;] could be written
// [?x], and its implicit type would be Entity. 
//
// Question: What's the difference between [?x] and ?x ? Latter is illegal
//           here! A bare bound label is only permitted in an arc.
//
// Note: The final optional comment is actually handled by the descriptor
//       production's invocation of the cg production, since a comment is
//       in fact a CG.
//
// An entry point to the parser.

concept returns [Concept c]
{
	// Get current context scope and push a new one
	// for this concept in case it turns out to be
	// a context. If it's not a context, no harm will
	// be done since it will simply not contain any
	// defining labels.
	ContextScope scope = contexts.peek();
	contexts.push(new ContextScope());

	// Concept and its components.
	c = new Concept();
	Concept bc = null; // bound concept from a coreference link
	String label = null;
	ConceptType ct = null;
	Referent r = null;
	Graph desc = null;
    String comment = null;
} 		      
			  : LBRACK
                (
					label = type //[1] 
					{
						// Set concept type, if a simple type name.
						// This is the only kind of type we'll handle
						// for now.
						if (label != null) {
							ct = conceptTypes.getTypeByLabel(label);
							if (ct == null) {
								ct = new ConceptType(label);
								conceptTypes.addTypeToHierarchy(ct);
							}
							c.setType(ct);
						}
					}				   					
				)?
				{
					// No concept type: Entity (Universal).
					if (ct == null) {
						ct = conceptTypes.getTypeByLabel("Universal");
						c.setType(ct);
					}
				}

			    ( options { warnWhenFollowAmbig=false; } :
				  (referent[c] corefLinks[scope, c]) =>
							(referent[c] bc = corefLinks[scope, c])
			    | (corefLinks[scope, c] referent[c]) =>
							(bc = corefLinks[scope, c] referent[c])
		        | (referent[c]) => referent[c]
			    | bc = corefLinks[scope, c] )?

		        {
					// A bound coreference label overrides the
					// newly created concept.
				    if (bc != null) c = bc;
				}

			    (COMMENT)? // can't store this in notio.Concept, so ignore?

               RBRACK
			   {
			       contexts.pop(); // remove the current context's scope
			   }
			   ;

// Conjuncts.
// A conjunction list consists of one or more type terms separated by "&". 
//
// The conjunction list must have the same valence N as every type term. 

conjuncts //[int valence]
			: typeTerm/*[valence]*/ ( AMPERSAND typeTerm/*[valence]*/ )*
			;

// CorefLinks.
// Coreference links are either a single defining coreference label or a
// sequence of zero or more bound labels. 
//
// If a dominant concept node, as specified in Section 6.9, has any
// coreference label, it must be either a defining label or a single bound
// label that has the same identifier as the defining label of some co-nested
// concept.
//
// Question: Do multiple bound labels signify multiple concepts which
//           constitute a descriptor? More likely, more than one bound
//           label in this context is probably an error. The latter is
//           the only case we'll deal with for now.

corefLinks[ContextScope scope, Concept c] returns [Concept boundConcept]
{
	Concept con = null;
	boundConcept = null;
	int count = 0;
}
			:  defLabel[scope, c]

			|  (con = boundLabel {count++;})*
			   {
				   // Only take notice of this if there is one bound label,
				   // for now. This entails [?x] which is the same as ?x, 
				   // whereas I'm not clear about the meaning of something 
				   // like [?x ?y].
				   if (count == 1) {
				       boundConcept = con;
				   }
			   }
			;

// DefLabel.
// A defining label is an asterisk "*" followed by an identifier. 
//
// The concept in which a defining label appears is called the defining 
// concept for that label; a defining concept may contain at most one defining
// label and no bound coreference labels. Any defining concept must be a
// dominant concept as defined in Section 6.9. 
//
// Every bound label must be resolvable to a unique defining coreference
// label within the same context or some containing context. When conceptual
// graphs are imported from one context into another, however, three kinds of 
// conflicts may arise: 
//
//          1. A defining concept is being imported into a context that is 
// within the scope of another defining concept with the same identifier. 
//
//          2. A defining concept is being imported into a context that
// contains some nested context that has a defining concept with the same
// identifier.
//
//          3. Somewhere in the same module there exists a defining concept
// whose identifier is the same as the identifier of the defining concept
// that is being imported, but neither concept is within the scope of the
// other. 
//
//      In cases (1) and (2), any possible conflict can be detected by
// scanning no further than the right bracket "]" that encloses the context
// into which the graph is being imported. Therefore, in those two cases,
// the newly imported defining coreference label and all its bound labels
// must be replaced with an identifier that is guaranteed to be distinct.
// In case (3), there is no conflict that could affect the semantics of the
// conceptual graphs or any correctly designed CG tool; but since a human
// reader might be confused by the similar labels, a CG tool may replace the 
// identifier of one of the defining coreference labels and all its bound
// labels. 
//
// Note: There are possibly subtle issues that the following simple
//       implementation does not address.

defLabel[ContextScope enclosingContext, Concept theConcept]
			:  STAR label:IDENTIFIER
			   {
				   String id = #label.getText();
				   /*
					* Ignore such transgressions for now. In some cases
					* they actually make sense.
				   if (enclosingContext.get(id) != null) {
					   String msg = "label '*" + id + "' already defined " +
				                    "in this context.";
					   throw new SemanticException(msg);
				   }
				   */
				   enclosingContext.def(id, theConcept);
			   }
			;

// Descriptor.
// A descriptor is a structure or a nonempty CG. 
//
// A context-free rule, such as this, cannot express the condition that a CG
// is only called a descriptor when it is nested inside some concept. 

descriptor[Referent r]
{
	Graph desc = null;
}
			:  structure // this also returns a graph (i.e. a list of arcs)
			|  desc = cg { r.setDescriptor(desc); }
			;

// Designator.
// A designator is a literal, a locator, or a quantifier. 

designator[Referent r]
{
	Object lit = null;
	Designator designator = null;
	QuantifierMacro macro = null;
}
			:  lit = literal
			   {
				   MarkerSet mSet = kBase.getMarkerSet(); // really use this?
			       r.setDesignator(new LiteralDesignator(lit, mSet)); 
			   }

			|  designator = locator
			   {
				   r.setDesignator(designator);	
			   }

			|  macro = quantifier
			   {
					// A call to setQuantifier() seems to set the
				    // descriptor also. Whether this is a bug in
				    // Notio or pCG I'm not sure, but the former
				    // seems likely. Checking that a bogus one hasn't
					// been set and if it has, unsetting it, doesn't
				    // seem to make any difference.
					r.setQuantifier(macro);
			   }
			;

// Disjuncts.
// A disjunction list consists of one or more conjunction lists separated by
// "|". 
//
// The disjunction list must have the same valence N as every conjunction
// list.

disjuncts //[int valence]
	        : conjuncts//[valence]
			  ( VERTBAR conjuncts/*[valence]*/ )*
			;

// FormalParameter.
// A formal parameter is a monadic type followed by an optional defining
// label. 
//
// The defining label is required if the body of the lambda expression
// contains any matching bound labels. 

formalParameter
{
	String label = null;
}
			:  label = type/*[valence]*/ ( defLabel[null, null] )?
			;

// Indexical.
// An indexical is the character "#" followed by an optional identifier. 
//
// The identifier specifies some implementation-dependent method that may
// be used to replace the indexical with a bound label. 

indexical returns [String s]
{
	s = null;
}
			:  HASH ( str:IDENTIFIER { s = #str.getText(); } )?
			   { if (s == null) s = "#"; }
			;

// IndividualMarker.
// An individual marker is the character "#" followed by an integer. 
//
// The integer specifies an index to some entry in a catalog of individuals. 

individualMarker returns [String s]
{
	s = null;
}
			:  HASH str:UNSIGNEDINT { s = #str.getText(); }
			;

// LambdaExpression(N). 
// A lambda expression begins with "(" and the keyword "lambda", it continues 
// a signature and a conceptual graph, and it ends with ")". 
//
// A lambda expression with N formal parameters is called an N-adic lambda 
// expression. The simplest example, represented "(lambda ())", is a 0-adic
// lambda expression with a blank CG.

lambdaExpression//[int arity]
{
	Graph body = null;
}
			:  LPAREN "lambda" signature/*[arity]*/ body = cg RPAREN
			;

// Literal.
// A literal is a number or a quoted string. 

literal returns [Object lit]
{
	lit = null;
}
			:  lit = number
			|  str:QUOTEDSTR { lit = #str.getText(); }
			;

// Locator.
// A locator is a name, an individual marker, or an indexical. 

locator returns [Designator d]
{
	d = null;
	String indiv = null;
	String indexical = null;
}			
			:  str:NAME
			   { d = new NameDesignator(#str.getText()); }

			|  (individualMarker) => indiv = individualMarker
			   {
				   MarkerSet mSet = kBase.getMarkerSet(); // really use this?
			       d = new MarkerDesignator(new Marker(mSet, indiv));
			   }

			|  indexical = indexical
			   {
				   MarkerSet mSet = kBase.getMarkerSet(); // really use this?
			       d = new MarkerDesignator(new Marker(mSet, indexical));
			   }			   
			;

// Negation.
// A negation begins with a tilde "~" and a left bracket "[" followed by a
// conceptual graph and a right bracket "]". 
//
// A negation is an abbreviation for a concept of type Proposition with an
// attached relation of type Neg. It has a simpler syntax, which does not
// permit coreference labels or attached conceptual relations. If such options 
// are required, the negation can be expressed by the unabbreviated form
// with an explicit Neg relation. 

negation returns [Relation r]
{
	r = null;
	Graph desc = null;
}
			:  NEGATION_PREFIX desc = cg RBRACK
		       {
				   Concept[] args = new Concept[1];
				   String propLabel = "Proposition";
			       ConceptType ct = conceptTypes.getTypeByLabel(propLabel);
				   if (ct == null) {
				       ct = new ConceptType(propLabel);
					   conceptTypes.addTypeToHierarchy(ct);				  
				   }
				   args[0] = new Concept(ct);
				   args[0].setReferent(new Referent(desc)); // set descriptor
				   // (Neg [Proposition: desc])
				   String negLabel = "Neg";
				   RelationType rt = relationTypes.getTypeByLabel(negLabel);
				   if (rt == null) {
						rt = new RelationType(negLabel, 1);
						relationTypes.addTypeToHierarchy(rt);
				   }
				   r = new Relation(rt, args);
			   }
			;

// Number.
// This is actually a lexical category the top-level of which seems to be
// best handled in the parser rather than the lexer.

number returns [Number num]
{
	num = null;
}
		    : n:INT_NUM
			{
			    String s = #n.getText();
				// Java doesn't permit an integer to be preceded by "+".
				// This is not the case for floating point values!
				if (s.startsWith("+")) { s = s.substring(1); }
			    Long l;
			    try {
				    l = Long.valueOf(s);
				    num = l;
			    } catch(NumberFormatException e) {
				    throw new SemanticException(s + ": illegal number format");
				}
		    }

		    | m:FLOAT_NUM
		    {
			    String s = #m.getText();
			    Double d;
			    try {
				    d = Double.valueOf(s);
				    num = d;
			    } catch(NumberFormatException e) {
				   throw new SemanticException(s + ": illegal number format");
				}
	        }
		    ;

// Quantifier.
// A quantifier consists of an at sign "@" followed by an unsigned integer or 
// an identifier and an optional list of zero or more arcs enclosed in braces. 
//
// The symbol @some is called the existential quantifier, and the symbol 
// @every is called the universal quantifier. If the quantifier is omitted, 
// the default is @some. 
//
// Question: The first sentence above is ambiguous. Does it mean that only
//           the identifier can be followed by the brace enclosed list? In
//           other words, does "and" have a higher precedence than "or" in
//           the description? I'm assuming that this is the case.

quantifier returns [QuantifierMacro m]
{
	m = null;
	String name = null;
	ArrayList list = new ArrayList();
}				
				: AT
				( n:UNSIGNEDINT
					{
						String s = null;
			    		try {
							s = #n.getText();
				    		int num = Integer.valueOf(s).intValue();
							m = new NumericQuantifier(num);
			    		} catch(NumberFormatException e) {
							throw new SemanticException(s + 
											": illegal number format");
						}
					}

				| id:IDENTIFIER
				  {
					  name = #id.getText();
					  if (name.equals("some") || name.equals("every")) {
						  // canonicty for existential & universal quantifiers
						  name = name.toLowerCase();
					  }
				  }

				  // ANTLR complains about a nondeterminism between
				  // the following expression (alt 1) and a second alt
			      // presumably in the optional block. Since the second
				  // alt is the empty string, there should be no problem
				  // with turning off the warning. Compare this scenario
				  // against the Greedy Subrules section of the ANTLR 2.7.0
				  // docs on page 18.
				  ( options { warnWhenFollowAmbig=false; /*greedy=true;*/ }
				  : LBRACE
				     str1:QUOTEDSTR { list.add(#str1.getText()); }
				     ( COMMA str2:QUOTEDSTR { list.add(#str2.getText());} )*
				    RBRACE 
					{
						// For example: @Col{"Foo", "Bar"}
						Object[] collection = list.toArray(new Object[0]);
					    m = new DefinedQuantifier(name, collection);
					}
				   )? 
				
				   {
				       // Named quatifier with no collection,
					   // e.g.@some, @every
				       if (m == null) m = new DefinedQuantifier(name);
				   }
				)
			;

// Referent.
// A referent consists of a colon ":" followed by an optional designator and
// an optional descriptor in either order. 

referent[Concept c]
{
	Referent r = new Referent(); // create a referent but only set it later
}	 		
			:  	COLON
                { c.setReferent(r); } // colon implies referent: add to concept
				( options { warnWhenFollowAmbig=false; } :
				  (designator[r] descriptor[r]) =>
						(designator[r] descriptor[r])
				| (descriptor[r] designator[r]) =>
						(descriptor[r] designator[r])
				| (descriptor[r]) => descriptor[r]
				| designator[r] )?
			;


// Relation.
// A conceptual relation begins with a left parenthesis "(" followed by an
// N-adic type, N arcs, and an optional comment. It ends with a right
// parenthesis ")". 
//
// The valence N of the relation type must be equal to the number of arcs.
//
// Note: Just as there are special context labels, there ought also to be
//       special relation labels, e.g. Neg for negations, and GT for type
//       hierarchies. Or is this not reasonable?

relation /*[int valence]*/ returns [Relation r]
{
	r = null;
	String label = null;
	ArrayList argList = new ArrayList();
	Concept c = null;
} 		      
			:   LPAREN
				label = type //[valence]

				( c = arc { argList.add(c); } )*

				(COMMENT)? // can't store this in notio.Relation, so ignore?

				RPAREN
				{
					// Create a relation given above type and arguments.
					Concept[] args=(Concept[])argList.toArray(new Concept[0]);
					RelationType rt = relationTypes.getTypeByLabel(label);
					if (rt == null) {
						rt = new RelationType(label, args.length);
						relationTypes.addTypeToHierarchy(rt);
					}
					r = new Relation(rt, args);

					// Add to type lattice. The currently active KB at the
					// time of parsing is the beneficiary of this, i.e. a
					// KnowledgeBase context doesn't in any sense create
					// a KB here. Only handle the greater-than relation
					// on types currently. Is this reasonable? This code
					// should probably be moved out of the parser. One may
					// also want to be able to turn this feature off.
					
					if (args.length == 2) {
					  String label1 = args[0].getType().getLabel();
					  String label2 = args[1].getType().getLabel();
					  if (label.equals("GT") && args.length == 2 &&
						(label1.equals("TypeLabel") &&
						 label2.equals("TypeLabel") ||
						 label1.equals("RelationLabel") &&
						 label2.equals("RelationLabel"))) {
						Referent ref1, ref2;
						Designator d1, d2;
						String t1, t2;
						String msg = "Invalid type designator.";

						ref1 = args[0].getReferent();
						d1 = ref1.getDesignator();
						if (d1 == null ||
							d1.getDesignatorKind() !=
							Designator.DESIGNATOR_LITERAL) {
							throw new SemanticException(msg);
						} else {
							Object o = ((LiteralDesignator)d1).getLiteral();
							if (o instanceof String) {
								t1 = (String)o;
							} else {
								throw new SemanticException(msg);
							}
						}
  
						ref2 = args[1].getReferent();
						d2 = ref2.getDesignator();
						Macro q = ref2.getQuantifier();
						if (d2 != null && d2.getDesignatorKind() ==
							Designator.DESIGNATOR_LITERAL) {
							Object o = ((LiteralDesignator)d2).getLiteral();
							if (o instanceof String) {
								// t1 > t2
								t2 = (String)o;
								if (label1.equals("TypeLabel")) {
								  ConceptType ct1;
								  ct1 = conceptTypes.getTypeByLabel(t1);
								  if (ct1 == null) {
									ct1 = new ConceptType(t1);
									conceptTypes.addTypeToHierarchy(ct1);
								  }
								  ConceptType ct2;
								  ct2 = conceptTypes.getTypeByLabel(t2);
								  if (ct2 == null) {
									ct2 = new ConceptType(t2);
									conceptTypes.addTypeToHierarchy(ct2);
								  }
								  conceptTypes.addSubTypeToType(ct1, ct2);
								} else {
								  RelationType rt1;
								  rt1 = relationTypes.getTypeByLabel(t1);
								  if (rt1 == null) {
									rt1 = new RelationType(t1);
									relationTypes.addTypeToHierarchy(rt1);
								  }
								  RelationType rt2;
								  rt2 = relationTypes.getTypeByLabel(t2) ;
								  if (rt2 == null) {
									rt2 = new RelationType(t2);
									relationTypes.addTypeToHierarchy(rt2);
								  }
								  relationTypes.addSubTypeToType(rt1, rt2);
								}
							} else {
								throw new SemanticException(msg);
							}
						} else if (q != null) {
							Macro macro = q;
							if (macro instanceof DefinedQuantifier) {
								Object[] res = macro.executeMacro(null);
								if (res != null) {
									// t1 > t2a, t2b, t2c ...
									ConceptType ct1 = null;
									RelationType rt1 = null;
									if (label1.equals("TypeLabel")) {
									  ct1 = conceptTypes.getTypeByLabel(t1);
									  if (ct1 == null) {
										ct1 = new ConceptType(t1);
										conceptTypes.addTypeToHierarchy(ct1);
									  }
									} else {
									  rt1 = relationTypes.getTypeByLabel(t1);
									  if (rt1 == null) {
										rt1 = new RelationType(t1);
										relationTypes.addTypeToHierarchy(rt1);
									  }
									}

									for (int i=0;i<res.length;i++) {
										t2 = (String)res[i];
										ConceptType ct2 = null;
										RelationType rt2 = null;
										if (label1.equals("TypeLabel")) {
									      ct2=conceptTypes.getTypeByLabel(t2);
									      if (ct2 == null) {
											ct2 = new ConceptType(t2);
											conceptTypes.
												addTypeToHierarchy(ct2);
										  }
										  conceptTypes.
										      addSubTypeToType(ct1, ct2);
										} else {
									      rt2=relationTypes.getTypeByLabel(t2);
									      if (rt2 == null) {
											rt2 = new RelationType(t2);
											relationTypes.
												addTypeToHierarchy(rt2);
										  }
										  relationTypes.
											  addSubTypeToType(rt1, rt2);
										}
									}
								} else {
									throw new SemanticException(msg);
								}
							} else {
								throw new SemanticException(msg);
							}
						} else {
							throw new SemanticException(msg);
						}
					  }
                    }
				}
			;

// Signature.
// A signature is a parenthesized list of zero or more formal parameters
// separated by commas. 

signature  	:  LPAREN ( formalParameter (COMMA formalParameter )* )? RPAREN
			;

// SpecialConLabel.
// A special context label is one of five identifiers: "if", "then", "either",
// "or", and "sc", in either upper or lower case. 
//
// The five special context labels and the two identifiers "else" and "lambda"
// are reserved words that may not be used as type labels. 

specialConLabel
			: "if" | "then" | "either" | "or" | "sc"
			;

// SpecialContext. 
// A special context is either a negation or a left bracket, a special context
// label, a colon, a CG, and a right bracket. 
//
// Note: As far as I can see, there's no provision in Notio to mark a context
//       as having a special type label.

specialContext[Graph g]
{
	Relation r = null;
	Graph desc = null;
}
			:  r = negation
			   {
				 try {
			       g.addRelation(r);
				 } catch(ConceptAddError e) {
					// Notio says we have a reference to the same
					// concept in two different graphs, so copy all
					// arguments just to be sure. Only happens when
					// adding a relation/actor to a graph not during
					// construction of the relation/actor.
					Concept[] args = r.getArguments();
					for (int i=0;i<args.length;i++) {
						r.replaceArgument(args[i],
										  args[i].copy(COPYING_SCHEME));
					}
					g.addRelation(r);
				 } catch (RelationAddError e) {
				    // Should never happen because we new all relations
					// unlike concepts which are sometimes taken from
					// a symbol table via bound labels.
				 }
			   }

			|  LBRACK label:specialConLabel COLON desc = cg RBRACK
			   {
				   // Create a concept whose type is one of the special
				   // context labels, and whose referent is a descriptor.
				   String scLabel = #label.getText().toLowerCase();
			       ConceptType ct = conceptTypes.getTypeByLabel(scLabel);
				   if (ct == null) {
				       ct = new ConceptType(scLabel);
					   conceptTypes.addTypeToHierarchy(ct);				  
				   }

				   try {
			         g.addConcept(new Concept(ct, new Referent(desc)));
				   } catch (ConceptAddError e) {
					// Ignore this for now. See a *possibly* similar
					// scenario in the cg production.
				   }
			   	}
			;

// Structure.
// A structure consists of an optional percent sign "%" and identifier
// followed by a list of zero or more arcs enclosed in braces. 
//
// Question: Shouldn't "(arc)*" be replaced by "(QUOTEDSTR)*" here?

structure  	
{
	Concept c = null;
}
			:  ( PERCENT IDENTIFIER )? LBRACE (c = arc)* RBRACE
			;

// Type.
// A type is a type expression or an identifier other than the reserved
// labels: "if", "then", "either", "or", "sc", "else", "lambda". 
//
// A concept type must have valence N=1. A relation type must have valence N
// equal to the number of arcs of any relation or actor of that type. The
// type label or the type expression must have the same valence as the type. 
//
// Note: Currently we only handle the semantics of simple named types and
//       so return a string.

type returns [String label] //[int valence]
{
	label = null;
}
			: str:typeLabel { label = #str.getText(); }//[valence] 
			| typeExpression //[valence]
			;

// TypeExpression.
// A type expression is either a lambda expression or a disjunction list
// enclosed in parentheses. 
//
// The type expression must have the same valence N as the lambda expression 
// or the disjunction list.

typeExpression //[int valence]
			: (LPAREN "lambda") => lambdaExpression //[valence]
			| LPAREN disjuncts /*[valence]*/ RPAREN
			;

// TypeLabel.
// A type label is an identifier. 
//
// The type label must have an associated valence N. 

typeLabel //[int valence]
			: IDENTIFIER
			;

// TypeTerm.
// A type term is an optional tilde "~" followed by a type. 
//
// The type term must have the same valence N as the type.

typeTerm //[int valence]
{
	String label = null;
}
			: (TILDE)? label = type //[valence]
			;

// -------------------------------------------------------------------

/** 
 * CGIF lexical analyser for pCG.
 *
 * David Benn, June 2001
 */

class AntlrCGIFLexer extends Lexer;

options {
    k=2;
    exportVocab=AntlrCGIF;
    // Use general ASCII character set -- see ANTLR docs.
	// Should this be Unicode instead? See The CGIF Standard.
    charVocabulary = '\3'..'\377';
	//testLiterals=false; // nothing to check; all reserved words in rules
	caseSensitive=false;
}

// The August 7 1999 version of the Standard makes reference to character
// escape sequences, but the June 2001 Standard does not, so they are not
// defined here currently.

// Simple tokens.

LBRACK	: '[' ;

RBRACK	: ']' ;

LPAREN	: '(' ;

RPAREN	: ')' ;

LANGLE : '<' ;

RANGLE : '>' ;

LBRACE	: '{' ;

RBRACE	: '}' ;

DOT	: '.' ;

STAR : '*' ;

QUESTION : '?' ;

COLON : ':' ;

COMMA : ',' ;

HASH : '#' ;

AT : '@' ;

TILDE : '~' ;

PERCENT : '%' ;

VERTBAR : '|' ;

AMPERSAND : '&' ;

NEGATION_PREFIX : "~[" ;

// DelimitedStr(D).
// A delimited string is a sequence of two or more characters that begin and
// end with a single character D called the delimiter. Any occurrence of D
// other than the first or last character must be doubled. This is used for
// the comment, name, and quoted string productions below.

// Comment.
// A comment is a delimited string with a semicolon ";" as the delimiter. 
//
// Note: According to The CGIF Standard, the final semi-colon is not optional
//       but CharGer generates comments that have the final semi-colon missing.
//		 Semi-colon delimited comments do not seem to appear in the 7 August
//		 1999 version of The Standard. Where they *are* used in the Comments
//       section near the end of section 7.2 of the June 2001 Standard,
//       semi-colon delimited comments only *start* with a semi-colon, not
//       also ending with one. Should the final semi-colon be made optional?
//
//       The rule perhaps ought to be:
//         ';' (~(';' | ']' | ')' | linefeed))* (']' | ')' | linefeed)
//  
//       Conversely, the latter defines a CGComment production to be
//       /* ... */, i.e. a C comment, but the June 2001 Standard does not.
//       Until all CG tools synchronise on the new Standard, interoperability
//       problems will ensue.

COMMENT  :  ';'! (~';' | ";;")* ';'!
		 ;

// Identifier.
// An identifier is a string beginning with a letter or underscore "_" and
// continuing with zero or more letters, digits, or underscores. 
// Identifiers beginning with "_" followed by one or more digits are
// generated by the Gensym rule defined in Section 8.2. Such identifiers
// should be avoided unless they have been generated by a call to the Gensym
// rule. 

IDENTIFIER :  ( LETTER | '_' ) ( LETTER | DIGIT | '_' )*
	 	   ;

// Number.
// A number is an integer or a floating-point number.
//
// No need for a distinction between integer and floating tokens, even though
// we may want to return a long or double value, e.g. as a concept designator.
//
// Floating.
// A floating-point number is a sign ("+" or "-") followed by one of three 
// options: (1) a decimal point ".", an unsigned integer, and an optional
// exponent; (2) an unsigned integer, a decimal point ".", an optional
// unsigned integer, and an optional exponent; or (3) an unsigned integer
// and an exponent.
//
// Integer.
// An integer is a sign ("+" or "-") followed by an unsigned integer. 
//
// Question: is the non-optionality of the leading sign really correct? 

NUMBER
{
	_ttype = FLOAT_NUM;
}
	: ( '+' | '-' )
      ( ( '.' UNSIGNEDINT ( EXPONENT )? ) // {1}
      | (UNSIGNEDINT '.') => UNSIGNEDINT '.' (UNSIGNEDINT)? (EXPONENT)? // {2}
      | (UNSIGNEDINT EXPONENT) => UNSIGNEDINT EXPONENT // 3
	  | UNSIGNEDINT { _ttype = INT_NUM; } ) // {Integer}
	;

// UnsignedInt.
// An unsigned integer is a string of one or more digits. 

UNSIGNEDINT : ( DIGIT )+
	        ;

// Exponent.
// An exponent is the letter E in upper or lower case, an optional sign 
// ("+" or "-"), and an unsigned integer. 

protected
EXPONENT :
	'e' ('+' | '-')? UNSIGNEDINT
	     ;

// Name.
// A name is a delimited string with a single quote "'" as the delimiter.

NAME : '\''! (~'\'' | "''")* '\''!
	 ;

// QuotedStr.
// A quoted string is a delimited string with a double quote '"' as the
// delimiter. 

QUOTEDSTR : '"'! (~'"' | "\"\"")* '"'!
		  ;

// The following tokens are not specified in the CG Standard, but are
// required by the foregoing tokens.

protected
LETTER  :       'a'..'z' 
	    ;

protected
DIGIT   :       '0'..'9' 
	    ;

WS      :	((("\r\n" | '\r' | '\n') { newline(); })
			|	' '
			|	'\t' 
			|	'\f')   
                { _ttype = Token.SKIP; }   
	    ;
