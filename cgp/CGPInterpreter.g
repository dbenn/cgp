header {
    package cgp;

	import antlr.CommonAST;
    import antlr.SemanticException;

    import cgp.runtime.ActorException;
    import cgp.runtime.ActorType;
    import cgp.runtime.BooleanType;
    import cgp.runtime.ConceptType;
    import cgp.runtime.FileType;
    import cgp.runtime.FormalParameter;
    import cgp.runtime.FunctionType;
    import cgp.runtime.GraphType;
    import cgp.runtime.KBase;
    import cgp.runtime.KnowledgeBaseStack;
    import cgp.runtime.LambdaType;
    import cgp.runtime.LastException;
    import cgp.runtime.ListType;
    import cgp.runtime.Namespace;
    import cgp.runtime.NumberType;
    import cgp.runtime.ProcessType;
    import cgp.runtime.Rule;
    import cgp.runtime.ReturnException;
    import cgp.runtime.Scope;
    import cgp.runtime.ScopeStack;
    import cgp.runtime.StringType;
    import cgp.runtime.SubActorInfo;
    import cgp.runtime.Type;
    import cgp.runtime.UndefinedType;

    import java.io.File;
    import java.io.IOException;
    import java.util.Enumeration;
    import java.util.LinkedList;
    import java.util.List;
    import java.util.Properties;

    import notio.Actor;
    import notio.Concept;
    import notio.ConceptReplaceException;
    import notio.Graph;
    import notio.Relation;
    import notio.TypeAddError;
    import notio.TypeChangeError;
}

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
 * Conceptual Graph Processes interpreter.
 *
 * David Benn, June-October 2000, June 2001
 */

class CGPInterpreter extends TreeParser;

options {
    importVocab=CGP; // use vocab created by lexer and parser
    defaultErrorHandler=false;
}

{
    // ** Interpreter initialisation code.
    ScopeStack scopes;
    Scope topLevelScope;
    KnowledgeBaseStack kbases;
    KBase topLevelKBase;
    boolean trace;
	String[] args;
    CommonAST tree;

    public CGPInterpreter(String[] args, CommonAST tree) {
		this();
		this.trace = false;
		this.args = args; // pCG command-line arguments
        this.tree = tree;
    }
}

// ---------------------------------------------------------------------

program
{
    // ** Code that must be executed at the start of each 
    // ** complete program run.

    // Create a scope stack and make it available to all Type instances.
    // TBD: Probably should put such setters in CGP class.
    scopes = new ScopeStack();
    Type.setScopeStack(scopes);

    // Create the top-level scope.
    topLevelScope = new Scope();
    scopes.push(topLevelScope);

    // Create a knowledge base stack and make it available
    // to all Type instances.
    kbases = new KnowledgeBaseStack();
    Type.setKBStack(kbases);

    // Create the top-level knowledge base.
    topLevelKBase = new KBase();
    kbases.push(topLevelKBase);

    // Add environment variables to top-level scope for use
    // by pCG programs.
    ListType envars = new ListType();
    topLevelScope.def("_ENV", envars);
    Properties env = System.getProperties();
    Enumeration keys = env.propertyNames();
    while (keys.hasMoreElements()) {
		String key = (String)keys.nextElement();
		String value = env.getProperty(key);
		ListType pair = new ListType();
		pair.append(new StringType(key));
		pair.append(new StringType(value));
		envars.append(pair);
    }

	// Add command-line arguments to top-level scope for use
    // by pCG programs. Exclude name of the pCG program.
	ListType pCGArgs = new ListType();
    topLevelScope.def("_ARGS", pCGArgs);
	for (int i=1;i<args.length;i++) {
		pCGArgs.append(new StringType(args[i]));
	}
}
	  :	( topLevelStatement )*
          ;

topLevelStatement
          :     optionStatement
    	  |	typeDecl
	  |	funDef
          |     lambdaDef
          |     actorDef
          |     processDef
	  |	statement
          ;

optionStatement
{
	String theOpt = null;
	boolean CGIFParserOption = false;
	boolean CGIFGenOption = false;
}
	  :     #(OPTION_LIST
		(
		 opt:IDENT
		 {
		    theOpt = opt.getText();
		    if (theOpt.toUpperCase().equals("LFOUT")) {
			    CGP.LFOpt = true;
			} else if (theOpt.toUpperCase().equals("TRACE")) {
			    CGP.setTraceMode(true);
				trace = true;
				System.out.println("TRACE: " + tree.toStringList());
			} else if (theOpt.toUpperCase().equals("CGIFPARSER")) {
				// The option we're dealing with sets the CGIF parser.
				CGIFParserOption = true;
			} else if (theOpt.toUpperCase().equals("CGIFGEN")) {
				// The option we're dealing with sets the CGIF generator.
				CGIFGenOption = true;
			} else {
				String msg = "'" + theOpt + "' is an unknown option.";
				throw new SemanticException(msg);
			}
		 }
		 (
		  // Handle name-value pair form of the OPTION directive.
		  str:STRING
		  {
		    if (CGIFParserOption) {
				// Set the CGIF parser to something other than the
			    // default Notio CGIF parser.
			    CGP.CGIFParserOpt = str.getText();
            } else if (CGIFGenOption) {
				// Set the CGIF generator to something other than the
			    // default Notio CGIF generator.
			    CGP.CGIFGenOpt = str.getText();
            } else {
			    String msg = "'" + theOpt + "' option does not take the ";
				msg += "form of a name-value pair.";
				throw new SemanticException(msg);
			}
           }
          )?
		 )+
		)
	  ;

typeDecl  
{
    boolean isConceptDecl = false;
    String superTypeName = null;
}         
          :	#(TYPE_DECL t:nodeKind
		{
		    // A correct parse guarantees one of
		    // the values in nodeKind, so if it's
		    // not a concept declaration, it's a 
		    // relation declaration.
		    if (t.getText().equals("concept")) {
			isConceptDecl = true;
		    }
		}

		  id1:IDENT 
	          {
		      String typeName = id1.getText();

		      if (isConceptDecl) {
			  topLevelKBase.addConceptType(typeName);
		      } else {
			  topLevelKBase.addRelationType(typeName);
		      } 
		      
		      superTypeName = typeName;
		  }

		  ( id2:IDENT
		  {
		      String typeName = id2.getText();

		      if (isConceptDecl) {
			  topLevelKBase.linkConceptTypes(superTypeName,
							typeName);
		      } else {
			  topLevelKBase.linkRelationTypes(superTypeName,
							 typeName);
		      } 
		      
		      superTypeName = typeName;		      
		  })* 
		)
	  ;
          exception
	  catch[TypeAddError e] {
	      String msg = "error adding type.";
	      throw new SemanticException(msg);	      
	  }
          catch[TypeChangeError e] {
	      String msg = "error changing type.";
	      throw new SemanticException(msg);	      
          }

nodeKind  :     "concept" | "relation"
          ;

funDef	  
{
    LinkedList formals = new LinkedList();
}
	  :	#(FUN_DEF name:IDENT 
		( arg:IDENT
		  { formals.add(new FormalParameter(arg.getText())); }
		)*
		b:BLOCK
		{
		    FunctionType func = new FunctionType(name.getText(),
			     	      	    (FormalParameter[])formals.
					      toArray(new FormalParameter[0]),
			     	      	     b);
		    scopes.peek().def(name.getText(), func);
		})
	  ;

actorDef	  
{
    boolean isOut = false; // keep javac happy 
    LinkedList formals = new LinkedList();
    Type g = UndefinedType.undefined;
}
	  :	#(ACTOR_DEF name:IDENT 
		(
		 options {
		     warnWhenFollowAmbig=false;
	  	 }
		 : arg:IDENT
		  {
		      //isOut = kind.getText().equals("out") ? true : false; 
		      formals.add(new FormalParameter(arg.getText(), isOut)); 
		      // ??? "out" as in mutatable? We only really handle 
		      // "in" parameters to actors, getting a whole mutated
		      // graph as a whole.
		  }
		)+
		g=expr
		{
		    if (g instanceof GraphType) {
			try {			    
		            ActorType actor = new ActorType(name.getText(),
	  		                      (FormalParameter[])formals.
				      	       toArray(new FormalParameter[0]),
			     	      	      (GraphType)g);
			    // Assume the graph is valid and add it to the
			    // current scope so that recursive actors are
			    // possible. If it's not valid, the next step
			    // will cause a fatal error anyway.
			    scopes.peek().def(name.getText(), actor);
			    // Check whether graph is valid at definition 
			    // time.
			    actor.studyGraph();
		  	} catch(ActorException e) {
	    		    String msg = "actor is not well-formed: ";
			    msg += e.getMessage();
			    throw new SemanticException(msg);
			}
		    } else {
	    		String msg = "an actor's body must be a graph.";
			throw new SemanticException(msg);
		    }
		})
	  ;

actorParamKind
	  :	"in" | "out"
	  ;

lambdaDef
{
    LinkedList formals = new LinkedList();
    Type g = UndefinedType.undefined;
}
	  :	#(LAMBDA_DEF name:IDENT 
		( 
		 options {
		     warnWhenFollowAmbig=false;
	  	 }
		 : arg:IDENT
		   {
		      formals.add(new FormalParameter(arg.getText(), false)); 
		   }
		)+
		g=expr
		{
		    if (g instanceof GraphType) {
			LambdaType lambda = new LambdaType(name.getText(),
					   (FormalParameter[])formals.
					    toArray(new FormalParameter[0]),
					   (GraphType)g);	
			scopes.peek().def(name.getText(), lambda);
		    } else {
	    		String msg = "a lambda's body must be a graph.";
			throw new SemanticException(msg);
		    }
		})
	  ;

processDef
{
    boolean isOut = false;
    LinkedList formals = new LinkedList();
    AST initialBlock = null;
    LinkedList rules = new LinkedList();
    Rule rule = null;
    boolean negateGraph = false;
    LinkedList negateMatchGraphList = null;
    LinkedList matchList = null;
    boolean exportGraph = false;
    LinkedList exportGraphOptList = null;
    LinkedList mutateKBList = null;
}
	  :	#(PROCESS_DEF processName:IDENT
		// Parameter list.
		(
		 argKind:processParamKind arg:IDENT
		 {
		     String kind = argKind.getText();
		     isOut = kind.equals("out") ? true : false;  
		     formals.add(new FormalParameter(arg.getText(), isOut));
		 })*

		 // Optional initial code block, e.g. for setting
		 // up empty lists, counters etc.
		 ( 
		  b:BLOCK
		  {
		      initialBlock = b;
		  }
		 )?
		  
		 // Rule definitions.
		 (#(RULE_DEF ruleName:IDENT
		  {
		      rule = new Rule(ruleName.getText());
		      rules.add(rule);
		      matchList = new LinkedList();
		      negateMatchGraphList = new LinkedList();
		      exportGraphOptList = new LinkedList();
		      mutateKBList = new LinkedList();
		  }
		  
		  // Rule options.
		  (#(OPTION_LIST 
		   (
		    ruleOpt:IDENT
	            {
			String theOpt = ruleOpt.getText();
			if (theOpt.toUpperCase().equals("EXPORT")) {
			    rule.setExportAllOpt(true);
			} else if (theOpt.toUpperCase().
				   equals("EXPORTASSERT")) {
			    rule.setExportAssertOpt(true);
			} else if (theOpt.toUpperCase().
				   equals("EXPORTRETRACT")) {
			    rule.setExportRetractOpt(true);
			} else {
			    String msg = "'" + theOpt + 
				"' is an unknown option.";
			    throw new SemanticException(msg);
			}
		    }
		   )+
		  ))?

		    // Pre-condition definition.
		    //
		    // Note that while all pre-condition expressions
		    // must be graphs, this is not checked until
		    // process invocation time. We *could* check now,
		    // but that might have undesired side effects,
		    // especially if the expressions turned out to
		    // not be graphs. Then again, this would cause
		    // program termination so it may not matter if
		    // we did that check. The question is: is there
		    // any expression which evaluates to a graph in
		    // pCG wich could have undesirable side effects?
		    // The same comments hold for post-condition
		    // expressions.
		     #(PRE_DEF 
		       ( 
			actionBlockPre:BLOCK
		        {
			    rule.setPreconditionActionBlock(actionBlockPre);
			}
		       )?
		       (
		        {
			    negateGraph = false;
		        }
		        ( TILDE { negateGraph = true; } )?
			matchExpr:MATCH_EXPRESSION
		        {
			    negateMatchGraphList.add(new Boolean(negateGraph));
			    matchList.add(matchExpr);
		        }
		       )*			
		     {
			 rule.setMatchExpressions((AST[])matchList.
						  toArray(new AST[0]));
			 rule.setMatchGraphNegations(negateMatchGraphList);
		     })

		     // Post-condition definition.
		     #(POST_DEF 
		       ( 
			actionBlockPost:BLOCK
		        {
			    rule.setPostconditionActionBlock(actionBlockPost);
			}
		       )?
		       (
			mutateKBExpr:MUTATE_KB_EXPRESSION

			// Postcondition graph options.
		        {
			    exportGraph = false;
		        }
			(#(OPTION_LIST 
			 (
			  postCondOpt:IDENT
			  {
			      String theOpt = postCondOpt.getText();
			      if (theOpt.toUpperCase().equals("EXPORT")) {
				  exportGraph = true;
			      } else {
				  String msg = "'" + theOpt + 
				      "' is an unknown option.";
				  throw new SemanticException(msg);
			      }
			  }
			 )+)
		        )?
		        {
			    // Add a postcondition graph to this rule's list.
			    mutateKBList.add(mutateKBExpr);
			    // Set options for this graph.
			    // May be more such calls later.
			    exportGraphOptList.add(new Boolean(exportGraph));
		        }
		       )* // end of mutateKBExpression			
		     {
			 // Set postcondition graph list for this rule.
			 rule.setMutateKBExpressions((AST[])mutateKBList.
						     toArray(new AST[0]));
			 // Set postcondition graph options for this list.
			 rule.setMutateGraphExportOpts(exportGraphOptList);
		     })
		  )
                 )*

	         {
		     ProcessType process;
		     process = new ProcessType(processName.getText(),
				   (FormalParameter[])formals.
				    toArray(new FormalParameter[0]),
				   initialBlock,
				   (Rule[])rules.toArray(new Rule[0]));
		     scopes.peek().def(processName.getText(), process);
		 })
	  ;

processParamKind
	  :	"in" | "out"
	  ;

// ---------------------------------------------------------------------

statement 
{
    Type r = UndefinedType.undefined;
}
	  :	intrinsicStatement
	  |	ifStatement
	  |	whileStatement
	  |	foreachStatement
	  |	memberFunCall
	  |	r=call // ignore return value for statement form of call
	  |	assignment
	  ;

intrinsicStatement
{
    Type a = UndefinedType.undefined;
    Type b = UndefinedType.undefined;
    int exitCode = 0;
}
	  :	#("print" a=expr)
		{ System.out.print(a); }

	  |	#("println" a=expr)
		{ System.out.println(a); }

	  |	#("return" ( a=expr { scopes.peek().setReturnValue(a); } )?
		{ 
		    if (scopes.depth() > 1) {
			throw new ReturnException(); 
		    } else {
			String msg = "return can only be invoked from ";
			msg += "within a function, actor, or process.";
			throw new SemanticException(msg);
		    }
		})

	  |	#("exit" ( a=expr 
	        {
		    if (a instanceof NumberType) {
			// Make sure we have an integer exit code.
			exitCode = (int)((NumberType)a).round().getValue();
		    } else if (a instanceof StringType) {
			// Send error message to stderr and exit with a
			// non-zero value to indicate error to caller, 
			// e.g. shell.
			String msg = ((StringType)a).getValue();
			System.err.println(msg);
			exitCode = 1;
		    } else {
			String msg = "exit value must be a number or string.";
			throw new SemanticException(msg);
		    }
		})?
                {
		    System.exit(exitCode);
		})

          |     #("last" { throw new LastException(); })

          |     #("system" a=expr { systemStatement(_t, a); })

	  |	#("assert" a=expr 
	  	{ 
		    if (a instanceof GraphType) {
			kbases.peek().assertGraph((GraphType)a);
		    } else {
			String msg = "only graphs may be asserted.";
			throw new SemanticException(msg);
		    }
		})

	  |	#("retract" a=expr 
	  	{ 
		    if (a instanceof GraphType) {
			kbases.peek().retract((GraphType)a);
		    } else {
			String msg = "only graphs may be retracted.";
			throw new SemanticException(msg);
		    }
		})

	  |	#("apply" a=expr b=expr { funApply(_t, a, b); })
	  ;

systemStatement [Type a] returns [NumberType r]
{
    r = new NumberType(-1); // default to some error condition

    if (a instanceof StringType) {
	Process p = null;
	try {
	    String command = ((StringType)a).getValue();
	    p = Runtime.getRuntime().exec(command);
	    try {
		p.waitFor(); // block pCG program until the command exits
	    } catch (InterruptedException e) {
		// Do nothing except get return value after catch block.
	    }
	    r = new NumberType(p.exitValue());
	} catch (IOException e) {
	    r = new NumberType(p.exitValue());
	} 
    } else {
	String msg = "system command must be a string.";
	throw new SemanticException(msg);
    } 
}   	:
    	;

funApply [Type a, Type b] returns [Type r]
{
    r = UndefinedType.undefined;

    if (a instanceof FunctionType && b instanceof ListType) {
	FunctionType f = (FunctionType)a;
	ListType actuals = (ListType)b;
	r = funCall(_t, f, actuals.getValue());
    } else {
	String msg = "function and argument list" +
	" expected, " + a.getType() + 
	" and " + b.getType() + " found.";
	throw new SemanticException(msg);
    }
}  	:
	;

ifStatement
{
    Type a = UndefinedType.undefined;
    boolean cond = false;
}
	  : #(IF_STATEMENT c:CONDITION thenBlock:BLOCK
	    { 
		a = condition(c);
		if (a instanceof BooleanType) {
		    cond = ((BooleanType)a).getValue();
		    if (cond) {
			block(thenBlock);
		    }
		} else {
		    String msg = "expected boolean, found " + a.getType();
		    throw new SemanticException(msg);
		} 
	    }
	    ( elseBlock:BLOCK { if (!cond) block(elseBlock); } )?
	    )
	  ;

whileStatement
{
    Type a = UndefinedType.undefined;
    boolean done = false;
}
	  : #(WHILE_STATEMENT c:CONDITION b:BLOCK
	    {
		while (!done) {
		    a = condition(c);
		    if (a instanceof BooleanType) {
			if (((BooleanType)a).getValue() == true) {
			    try {
				block(b);
			    } catch (LastException e) {
				// a last statement was executed
				done = true;
			    }
			} else {
			    done = true;
			}
		    } else {
			String msg = "expected boolean, found " + a.getType();
			throw new SemanticException(msg);
		    }
		}
	    })
	  ;

foreachStatement
{
    Type a = UndefinedType.undefined;
    boolean done = false;
}
	  : #(FOREACH_STATEMENT id:IDENT a=expr b:BLOCK
	    {
		if (a instanceof ListType) {
		    String var = id.getText();
		    List list = ((ListType)a).getValue();
		    for (int i=0;i<list.size() && !done;i++) {
			Type x = (Type)list.get(i);
			scopes.peek().def(var, x);
			try {
			    block(b);
			} catch (LastException e) {
			    // a last statement was executed
			    done = true;
			}
		    }
		} else {
		    String msg = "expected list, found " + a.getType();
		    throw new SemanticException(msg);
		}
	    })	
          ;

condition returns [Type r]
{
    Type c = UndefinedType.undefined;
    r = UndefinedType.undefined;
}
	  : 	#(CONDITION c=expr) { r = c; }
	  ;

block 	  :	#(BLOCK ( statement )*)
	  ;

memberFunCall
{
    LinkedList args = null;
    Type obj = UndefinedType.undefined;
}
	  :	#(MEMBER_FUNCALL obj=expr name:IDENT args=actualArgs)
		{ obj.invokeMemberFunc(name.getText(), args.toArray()); }
	  ;

// This rule caters for the invocation of user defined functions, actors,
// and processes. Note that given that the actor type is a subclass of 
// the lambda type, test the former first lest an actor be treated as 
// a function.
call returns [Type r]
{
    LinkedList args = null;
    Type obj = UndefinedType.undefined;
    r = UndefinedType.undefined;
}
	  : 	#(ast:CALL obj=expr args=actualArgs)
		{
		    if (obj instanceof ActorType) {
			// re: use of #ast see generated Java code!
			r = actorCall(#ast, obj, args, false); 
		    } else if (obj instanceof LambdaType) {
			// re: use of #ast see generated Java code!
			r = lambdaCall(#ast, obj, args); 
		    } else if (obj instanceof FunctionType) {
			// re: use of #ast see generated Java code!
			r = funCall(#ast, obj, args); 
		    } else if (obj instanceof ProcessType) {
			// re: use of #ast see generated Java code!
			r = processCall(#ast, obj, args); 
		    } else {
			String msg = "expecting function, lambda, actor, ";
			msg += "or process; found " + obj.getType() + ".";  
			throw new SemanticException(msg);
		    }   
		}
	  ;

// Invoke a pCG function.
//
// ANTLR rule treated as a function to modularise the call rule.
// It would be preferable to situate this code in FunctionType
// but one attempt to do so resulted in incorrect interpretation,
// e.g. a complaint that multiplication could not be performed
// on values of type number!
//
funCall[Type obj, LinkedList args] returns [Type r]
{
    FormalParameter[] formals = null;
    Type[] actuals = new Type[0];
    r = UndefinedType.undefined;
    FunctionType func = (FunctionType)obj;

    // Get formal and actual parameter lists.
    formals = func.getFormals();
    actuals = (Type[])args.toArray(actuals);
    	
	// Proceed if there's no parameter length mismatch.
    if (formals.length == actuals.length) {
	    // Bind formal parameters to actuals in new scope. May be 0.
		Scope locals = scopes.push(new Scope());
		for (int i=0;i<formals.length;i++) {
			locals.def(formals[i].getName(), actuals[i]);
		}	

		// Define a "me" parameter which refers to this function.
		// This permits i) uniform error messages which refer to
		// the current function's name, and ii) anonymous recursive
		// functions.
		locals.def("me", func);
	
		// Is this a closure? If so, push the environment that existed
		// at the time of the definition. Note that this may override 
		// current variable values which may have "evolved" in the 
		// intervening period. Pushing this last ensures that the
		// closure's environment is updated, e.g. if variable x
		// exists in env and x is modified, it will be env's x, not
		// say, some x in the frame containing function parameters 
		// that will be modified. A consequence of pushing env last
		// is that local variables will now go into env, as will
		// return values. Another approach would simply be to
		// merge the locals scope above with this one. 
		Scope env = func.getEnv();
		if (env != null) {
			scopes.push(env);
		}
		
		// Evaluate the body of the function, handling
		// a return statement by catching an exception.
		try {				    	
			block(func.getCode());
		} catch(ReturnException e) {
			// Can get return value here or below.
			// Makes more sense here since r is 
			// otherwise undefined.
			r = scopes.peek().getReturnValue();
		}
		
		// Did we push an envionment above?
		if (env != null) {
			scopes.pop();
		} 

		// Pop function's local scope.
		scopes.pop();   
    } else {
		String msg = "parameter list length mismatch";
		throw new SemanticException(msg);
    }
}         :
	  ;

/**
 * Mutate a copy of an actor's defining graph by executing its sub-actors.
 *
 * A sub-actor cannot execute until all its input concept referents
 * are bound. A sub-actor is either a user-defined pCG function or
 * another actor, permitting recursive actors.
 *
 * Input concept referents are assumed to be pCG literals. Note
 * that this does not include marker or descriptor referents.
 *
 * User-defined functions are passed input and output concepts.  
 */
actorCall[Type obj, LinkedList args, boolean bindSinkToSource] returns [Type r]
{
    FormalParameter[] formals = null;
    Type[] actuals = new Type[0];
    ActorType actor = (ActorType)obj; // precondition: obj is an actor
    r = UndefinedType.undefined;

    // Get actual parameter list.
    //
    // If null, assume this actor's defining graph already 
    // has bound source concept designators. This would be
    // true where no explicit actor type definition is
    // present, i.e. an anonymous actor.
    if (args != null) {
	actuals = (Type[])args.toArray(actuals);
    }

    // In the case where an anonymous actor is created,
    // it is assumed to have been created with a zero-length
    // formals array. The actuals array will already
    // be zero length (as created above), except in the case 
    // of a recursive anonymous actor where the initial actor
    // will indeed have been created with a zero-length formals
    // array, but there will be actuals passed, and we will have
    // to bind these!! In this case, formals.length will be 0 
    // while actuals.length will be a positive integer. But,
    // since an anonymous recursive actor will have no name 
    // designators in its source concepts, there will be no
    // names to bind to, so unless some source argument order
    // is assumed, anonymous recursive actors cannot be fully
    // implemented.
    if (actor.getFormals().length == actuals.length || bindSinkToSource) {
	// Make a copy of the defining graph.
	actor = (ActorType)actor.copy();

	// Bind actuals by source concept designator formal names 
	// or by source concept ordering?
	if (bindSinkToSource) {
	    actor.bindParametersToSourceConcepts(actuals);
	} else if (actuals.length > 0) {
	    actor.bindParameters(actuals);
	}

	// If any source arguments remain unbound and a suitably named
	// coference variable is defined, bind the source argument's
	// designator to this coreference variable. This is primarily
	// motivated by the fact that some graph projections may not
	// always result in all concept designators being bound, and 
	// that existing coreference variables may fulfill this need.
	// Indeed, some of Mineau's examples rely upon such coreference
	// being global (and he says this should be the case). I have
	// restricted the scope to the current knowledge base however.
	// Should this be extended to unbound sink concepts after
	// actor activation? No, since this means that an actor failed
	// to generate a result!
	ConceptType[] sources = ActorType.getSources(actor.getBody());
	for (int i=0;i<sources.length;i++) {
	    if (sources[i].hasVarDesignator()) {
		String name =
		    ((StringType)sources[i].getDesignatorValue()).getValue();
		Type value = kbases.peek().getCorefVarValue(name);
		if (!(value instanceof UndefinedType)) {
		    sources[i].setDesignatorValue(value);
		} 
	    }
	}

	// Invoke the actor and each of its subactors.
	actor.initActorExecution();
	while (actor.isExecutable()) {
	    SubActorInfo info = actor.getNextSubActor();
	    if (info == null) {
		// There are sub-actors on the run-list but none 
		// is ready for execution, so stop and return what
		// we have.
		break;
	    }
	    
	    if (trace) {
		System.out.println("TRACE: Invoking sub-actor \"" + info +
				   "\" in actor \"" + actor.getId() 
				   + "\"");
		
		ConceptType[] concepts = ActorType.getSinks(actor.
							    getBody());
	    }
	    
	    if (info.executor instanceof FunctionType) {
		// Invoke a function, mutating the graph of the 
		// executor as a result.
		LinkedList funArgs = new LinkedList();
		funArgs.addAll(info.inArgs);
		funArgs.addAll(info.outArgs);
		funCall(_t, info.executor, funArgs); // _t unused; see src
	    } else if (info.executor instanceof ActorType) {
		// Get designators from info.inArgs to give input args.
		// It is a precondition that each inArgs member is of type
		// ConceptType. We'll pass Type values to the actor.
		LinkedList actorArgs = new LinkedList();
		for (int i=0;i<info.inArgs.size();i++) {
		    ConceptType input = (ConceptType)info.inArgs.get(i);
		    actorArgs.add(input.getDesignatorValue());
		}
		// Invoke actor with input args.
		// _t (same as #ast in call?) is unused. 
		// See source generated by ANTLR.
		// Note that a copy of the defining graph will be
		// made upon this recursive call.

		// Is this a recursive anonymous actor? If so, tell
		// actorCall() to directly bind actuals to source concept 
		// designators instead of binding by name.
		ActorType subActor = (ActorType)info.executor;
		boolean sinkToSource = false;
		if (subActor.isAnonymous() && info.isSelfReferential) {
		    sinkToSource = true;
		}
		GraphType g;
		try {
		    g = (GraphType)actorCall(_t, subActor, actorArgs,
					     sinkToSource);
		} catch (ClassCastException e) {
		    String msg = "sub-actor '" + subActor.getId() + 
			"' returned an undefined value.";
		    throw new SemanticException(msg);
		}
		
		// Get sink concepts from resulting graph which is a
		// copy of the original sub-actor's graph. The reason for
		// obtaining output concepts via getSinks() here is that
		// the graph may be arbitrarily complex, consisting of
		// nested sub-actors. Contrast the use of getSinks() with
		// ActorType.getNextSubActor().
		ConceptType[] sinks = ActorType.getSinks(g);
		
		// Set corresponding output concepts in info.outArgs
		// assuming the same ordering of output as sink concepts.
		// We *ought* to check the type compatability of the 
		// corresponding concept types. At least check the number
		// of arguments.
		if (sinks.length != info.outArgs.size()) {
		    String msg = "caller actor '" + actor.getId() + 
			"' and callee sub-actor '" + actor.getId() +
			"' differ in output parameter list lengths.";
		    throw new SemanticException(msg);
		} else {
		    for (int i=0;i<sinks.length;i++) {
			ConceptType output = (ConceptType)info.
			    outArgs.get(i);
			output.setDesignatorValue(sinks[i].
						  getDesignatorValue());
		    }
		}
		
		// Set the resulting graph to be the output of this
		// sub-actor invocation, since this now contains the 
		// appropriately mutated concepts. NO! We want the
		// values of the sinks in the returned copied graph!
		//actor = subActor;
	    } else {
		String msg = "a sub-actor must be a function or actor";
		throw new SemanticException(msg);
	    }
	}
	
	// Return the mutated graph copy.
	r = actor.getBody();
    } else {
	String msg = "parameter list length mismatch";
	throw new SemanticException(msg);
    }
}         :
	  ;

/**
 * Return a graph with name designators replaced by literal 
 * values or markers.
 */
lambdaCall[Type obj, LinkedList args] returns [Type r]
{
    FormalParameter[] formals = null;
    Type[] actuals = new Type[0];
    LambdaType lambda = (LambdaType)obj;
    r = UndefinedType.undefined;

    // Get actual parameter list.
    actuals = (Type[])args.toArray(actuals);

    if (lambda.getFormals().length == actuals.length) {
	// Get a copy of the defining graph.
	lambda = lambda.copy();

	// Bind source concept designators to actual parameters
	// and return the mutated graph copy.
	lambda.bindParameters(actuals);
	r = lambda.getBody();
    } else {
	String msg = "parameter list length mismatch";
	throw new SemanticException(msg);
    }
}         :
	  ;

/**
 * Invoke a process -- the main point of pCG.
 */
processCall[Type obj, LinkedList args] returns [Type r]
{
    FormalParameter[] formals = null;
    Type[] actuals = new Type[0];
    r = UndefinedType.undefined;
    ProcessType process = (ProcessType)obj;
    GraphType[] preParams = null;
    GraphType[] assertParams = null;
    GraphType[] retractParams = null;

    // Get formal and actual parameter lists.
    formals = process.getFormals();
    actuals = (Type[])args.toArray(actuals);
    
    if (formals.length == actuals.length) {
	// Create a new scope and KB.
	//
	// Whereas the new scope is used in conjunction with
	// scopes further down the stack as for functions, the 
	// new KB becomes the *active* one and its content is 
	// derived from the one it overrides, i.e. the one before 
	// it on the KB stack. So, the new KB benefits from the
	// graphs asserted previously, but does not pollute earlier
	// KBs, as per Mineau's suggestion that the operations
	// of a process should be local in scope.
	Scope locals = scopes.push(new Scope());
	KBase callerKBase = kbases.peek(); // in case option export enabled
	KBase theKBase = kbases.push(new KBase(kbases.peek()));

	// Define a "me" parameter which refers to this process.
	locals.def("me", process);
	
	// Handle actual parameters according to whether
	// the corresponding formal parameter is an input,
	// output, or trigger. All must be contexts, i.e
	// concepts with descriptor graphs as referents.
	//
	// Permitted "in" parameter concept types are: Proposition
	// and Condition, and permitted "out" parameter concept 
	// types are Proposition and Erasure. A major motivation for
	// requiring parameters to be concepts is that (apart from Mineau's
	// suggested usage) this is how they will appear in CGs anyway!
	LinkedList inP = new LinkedList();
	LinkedList assertP = new LinkedList();
	LinkedList retractP = new LinkedList();
	for (int i=0;i<formals.length;i++) {
	    if (actuals[i] instanceof ConceptType) {
		ConceptType p = (ConceptType)actuals[i];
		if (!p.isContext().getValue()) {
		    String msg = "parameters to a process must be contexts.";
		    throw new SemanticException(msg);		
		}

		// What is the context's type? Ignore its case.
		String contextType = p.getLabel().getValue().toUpperCase();

		// We know that a descriptor exists, so obtain it.
		// This is the graph we're actually interested in.
		GraphType pDesc = (GraphType)p.getDescriptor();
		
		if (formals[i].isIn()) {
		    if (contextType.equals("PROPOSITION")) {
			// Assert a trigger graph in this process's KB.
			theKBase.assertGraph(pDesc);
		    } else if (contextType.equals("CONDITION")) {
			// Collect graph for use in first rule's precondition.
			inP.add(pDesc);
		    } else {
			String msg = "process 'in' parameters must be " +
			    "contexts of type PROPOSITION or CONDITION.";
			throw new SemanticException(msg);
		    }
		} else if (formals[i].isOut()) {
		    if (contextType.equals("PROPOSITION")) {
			// Collect graph for use in last rule's postcondition.
			assertP.add(pDesc);
		    } else if (contextType.equals("ERASURE")) {
			// Collect graph for use in last rule's postcondition.
			//
			// Note: according to dpANS, this should really be:
			//
			//  (neg)->[ ... ] or ~[ ... ] 
			retractP.add(pDesc);
		    } else {
			String msg = "process 'out' parameters must be " +
			    "contexts of type PROPOSITION or ERASURE.";
			throw new SemanticException(msg);
		    }
		}
	    } else {
		String msg = "parameters to a process must be contexts.";
		throw new SemanticException(msg);		
	    }
	}	

	// Make in and out parameters (except triggers) available as arrays.
	preParams = (GraphType[])inP.toArray(new GraphType[0]);
	assertParams = (GraphType[])assertP.toArray(new GraphType[0]);
	retractParams = (GraphType[])retractP.toArray(new GraphType[0]);

	// Execute initial block if present.
	AST initialBlock = process.getInitialBlock();
	if (initialBlock != null) {
	    // Execute the block. A ReturnException
	    // may be thrown at which point the process
	    // will be exited. See catch block near end of
	    // this method.
	    block(initialBlock);
	}
	
	// Get the rule set.
	Rule[] rules = process.getRules();

	boolean exitRuleReached = false;

	try {				
	    // Continue to iterate over the rule set while there are
	    // matches occurring. If all rules are iterated over and
	    // no pre-condition of any rule matches, there is nothing
	    // left to do since the process will never change the state
	    // of the KB. On a given match iteration, if a matching rule
	    // is found, the for loop should be exited since we can't say
	    // where in the rule set a match on the next iteration (of
	    // the outer loop) will occur -- it could be the next rule
	    // in sequence, or the first rule, or anywhere in between.
	    boolean matchOccurred;
	    do {
		matchOccurred = false;

		// Iterate over each rule in the rule set, executing action 
		// blocks, looking for matching preconditions and executing 
		// post-condition blocks. There may be no rules in which case 
		// we'll never enter this loop, and wouldn't *that* be dull!
		for (int i=0;i<rules.length;i++) {
		    // Get next rule.
		    Rule rule = rules[i];
		    
		    // Execute precondition action block if present.
		    AST preActionBlock = rule.getPreconditionActionBlock();
		    if (preActionBlock != null) {
			// Execute the block. A ReturnException may be
			// thrown, at which point the process will exit.
			// See catch block near end of this method.
			block(preActionBlock);
		    }

		    // Innocent until proven guilty.
		    boolean allMatch = true;

		    // Collect all matches for a current rule test
		    // and make available as a special variable in
		    // the current scope.
		    ListType matches = new ListType();
		    scopes.peek().def("_MATCHES", matches);

		    // Determine whether all pre-condition graphs match.
		    // This includes preconditions passed as input parameters,
		    // which are assumed to be included in the rule zero.
		    // If one doesn't match, don't evaluate any more.
		    // If they all match, execute the post-condition block.

		    if (i == 0) {
			for (int j=0;j<preParams.length;j++) {
			    if (!matchFound(_t, preParams[j], theKBase, 
					    matches)) {
				allMatch = false;
				break;
			    }			    
			}
		    }

		    if (allMatch) {
			AST[] preconditions = rule.getMatchExpressions();
			for (int j=0;j<preconditions.length;j++) {
			    // Get the next (hopefully) graph to be matched.
			    Type value = matchExpression(preconditions[j]);

			    // Attempt a match.
			    boolean isMatch = 
				matchFound(_t, value, theKBase, matches);

			    // Is this graph to be negated?
			    if (rule.getMatchGraphNegation(j)) { 
				isMatch = !isMatch;
			    }

			    if (!isMatch) {
				allMatch = false;
				break;
			    }
			}
		    }
		    
		    if (allMatch) {
			// Execute postcondition action block if present.
			AST postActionBlock = 
			    rule.getPostconditionActionBlock();
			if (postActionBlock != null) {
			    // Execute the block. A ReturnException may be
			    // thrown, at which point the process will exit.
			    // See catch block near end of this method.
			    block(postActionBlock);
			}

			// Mutate the current KB with the postconditions.
			AST[] postconditions = rule.getMutateKBExpressions();
			for (int j=0;j<postconditions.length;j++) {
			    // Get the next (hopefully) singleton graph and
			    // its options.
			    Type value = mutateKBExpression(postconditions[j]);
			    boolean export = rule.getMutateGraphExportOpt(j);
			    mutateKB(_t, value, export, rule, 
				     theKBase, callerKBase);
			}

			matchOccurred = true;

			// The final rule is a special case. If its 
			// precondition matches, its postconditions will 
			// be handled above as normal, but then the process
			// will exit after any output parameters are 
			// asserted/retracted in the caller's KB, rather 
			// than restarting a search for matches at the start 
			// of the rule set again.
			//
			// TBD: It may be preferable to permit multiple rules 
			// to be designated exit (or export) points for 
			// increased flexibility. This is similar to the way 
			// in which a function may return at any point.
			
			if (i == rules.length-1) {
			    exitRuleReached = true;
			}
			
			// A rule has matched on this round, so start
			// at the top for the next matching round (unless
			// this is the last rule, as detailed above).
			break;
		    }
		}
	    } while (matchOccurred && !exitRuleReached);
	} catch(ReturnException e) {
	    // Get return value and pop current scope and KB.
	    // Under normal circumstances, this will be undefined
	    // since a process should end with assertions/retractions.
	    r = locals.getReturnValue();
	}

	// Cleanup the scope and KB stack.
	scopes.pop();    
	KBase processKB = kbases.pop();

	// If the final rule was matched, bind coreference variables in
	// the rule's postcondition graphs using the process's KB, then
	// assert/retract these graphs in the caller's KB.
	if (exitRuleReached) {
	    KBase callerKB = kbases.peek();

	    for (int i=0;i<assertParams.length;i++) {
		GraphType g = assertParams[i];
		processKB.bindCorefVars(g);
		callerKB.assertGraph(g, false); // false = don't bind vars
	    }

	    for (int i=0;i<retractParams.length;i++) {
		GraphType g = retractParams[i];
		processKB.bindCorefVars(g);
		callerKB.retract(g, false); // false = don't bind vars
	    }
	}
    } else {
	String msg = "parameter list length mismatch.";
	throw new SemanticException(msg);
    }
}         :
	  ;

// Simply returns a match expression for a process rule precondition.
// Hopefully this is a graph!
matchExpression returns [Type r]
{
    r = UndefinedType.undefined;
}
    	  :	#(MATCH_EXPRESSION r=expr)
          ;

// Simply returns a KB mutation expression for a process rule postcondition.
// Hopefully this is a context.
mutateKBExpression returns [Type r]
{
    r = UndefinedType.undefined;
}
    	  :	#(MUTATE_KB_EXPRESSION r=expr)
          ;

matchFound[Type value, KBase theKBase, ListType matches] 
returns [boolean matched]
{
    matched = false;

    if (value instanceof GraphType) {
	// Take a copy since it may be mutated below, e.g. see
	// isActorWithProjections case below.
	GraphType g = ((GraphType)value).copy();
	
	boolean isExact = false;
	boolean isValidProjection = false;
	boolean isActorWithProjections = false;
	
	// Is this a process invocation?
	// If so, simply invoke it. This case differs
	// from those below in that its invocation
	// is assumed to have some useful side effect
	// on the current KB and there is nothing to
	// match. Match defaults to true, as above.
	Type actorNode = getSingletonActorNode(_t, g);
	if (actorNode instanceof ProcessType) {
	    LinkedList args = g.getConcepts().getValue();
	    // In this context, any return value cannot 
	    // be utilised, so ignore it. 
	    Type r = processCall(_t, actorNode, args);
	} else {
	    // Try an exact match on each graph in the
	    // KB first, as this may be quite common.
	    // Otherwise try a projection on each graph
	    // in the KB. If this does not work, it may
	    // be because we are dealing with an actor
	    // being to glue together graphs from different
	    // parts of the KB. If this is so, we must
	    // attempt to project each graph in the
	    // actor graph against the KB's content.
	    // Note: I cannot see how anything other
	    // than an actor graph makes sense in the
	    // latter case, otherwise just what do we
	    // *mean* by a match?
	    isExact = theKBase.exactMatch(g);
	    if (isExact && !g.containsActorNodes()) { 
		// Collect non-actor graph with exact match.
		// If the graph is also an actor, it will be 
		// collected later (see below).
		matches.append(g);
	    }

	    if (!isExact) {
		GraphType pG = theKBase.projectionMatch(g);
		isValidProjection = pG != null;

		// Check that all variables are bound. This should
		// probably be done by the projection operation itself. (TBD)
		// It won't be valid to do this if the graph is an actor
		// since there may well be unbound variables!
		if (isValidProjection && !g.containsActorNodes()) {
		    ConceptType[] concepts = 
			(ConceptType[])pG.getConcepts().getValue().
			     toArray(new ConceptType[0]);
		    for (int i=0;i<concepts.length;i++) {
			if (concepts[i].hasVarDesignator()) {
			    isValidProjection = false;
			    break;
			}
		    }
		}

		if (isValidProjection) {
		    // Use this for future processing, i.e. actors.
		    // We could just rely upon coreferent variables being
		    // set by the projection's restrictions, but this is
		    // less efficient than using what's already there in
		    // the projection.
		    g = pG;
		    if (!g.containsActorNodes()) {
			// Collect non-actor graph with valid projection.
			// If the graph is also an actor, it will be 
			// collected later (see below).
			matches.append(g);
		    }		    
		}
	    }

	    if (!isExact && !isValidProjection && g.containsActorNodes()) {
		// Isolate each conceptual relation, create a graph
		// from it, and attempt a projection with it. The big
		// assumption here is that the actor's arguments are
		// part of single-relation graphs. If they are not, then 
		// the following is invalid since the whole sub-graph in
		// question (attached to an actor argument) should be
		// projected against the KB's graphs. So, a precondition
		// is that actor sources -- and sinks for that matter --
		// are single-relation graphs. The motto is, always
		// keep actors (and all graphs where possible), simple
		// for matching purposes.

		isActorWithProjections = true;
		Relation[] relations = g.getValue().getRelations();
		for (int i=0;i<relations.length;i++) {
		    // We don't examine actor relations, because
		    // it is assumed they will never be part of a
		    // graph in the KB. Another big assumption!
		    // If there are only actor relations, we have
		    // an otherwise blank graph which should match
		    // with anything! Flying thick and fast here!
		    // I've opened a can of works with respect to
		    // the meaning of "match".
		    //
		    // Once a projection is found, any coreference 
		    // variables from the projection's restrictions 
		    // will be bound to the desired values, for 
		    // subsequent use by the actor. In this case
		    // (as opposed to the single projection case
		    // above), doing so is simpler and more efficient
		    // than trying to bind the values in the original
		    // graph based upon the projection graph explicitly.
		    if (!(relations[i] instanceof Actor)) {
			GraphType subG = new GraphType(relations[i]);
			GraphType pG = theKBase.projectionMatch(subG);
			boolean isProjection = pG != null;
			
			if (!isProjection) {
			    isActorWithProjections = false;
			    break;
			}
		    }
		}
	    }
	    
	    if (isExact || isValidProjection || isActorWithProjections) {
		// Irrespective of the kind of match that took place, 
		// check for an actor in the graph and activate if present.
		if (g.containsActorNodes()) {
		   ActorType anonActor = new ActorType(g);
		   // Invoke actor with no arguments, since it is assumed
		   // to already have bound inputs.
		   GraphType h = (GraphType)actorCall(_t, anonActor, 
						      null, false);
		    
		   // Do the actual outputs match the target outputs? 
		   // Also need to check that original and current designators
		   // are not the same reference in case of a default value 
		   // having been used!
		    ConceptType[] targetSinks = ActorType.getSinks(g);
		    ConceptType[] actualSinks = ActorType.getSinks(h);
		    for (int k=0;k<targetSinks.length;k++) {
			Type targetDes, actualDes;
			targetDes = targetSinks[k].getDesignatorValue();
			actualDes = actualSinks[k].getDesignatorValue();
			if (trace) {
			    System.out.println("TRACE: " +
					       "\n target -> " + targetDes +
					       "\n actual -> " + actualDes +
					       "\n target type-> " + 
					       targetDes.getType() +
					       "\n actual type-> " + 
					       actualDes.getType() +
					       "\n target == actual: " +
					       (targetDes == actualDes) +
					       "\n target eq actual: " +
					       targetDes.equals(actualDes));
			}

			// A match occurs if one of the following is true:
			//
			// 1. The original sink had a variable designator
			//    (?x, *x) and the computed sink does not, i.e.
			//    the sink's referent has been computed.
			//
			// 2. The original sink didn't have a variable 
			//    designator and the value of the computed 
			//    sink's referent matches the original's,
			//    i.e. the original sink had a default
			//    referent value against which a comparison
			//    must be made.
			if (targetSinks[k].hasVarDesignator() &&
			    !actualSinks[k].hasVarDesignator()) {
			    String name = ((StringType)targetDes).getValue();
			    theKBase.addCorefVarMapping(name, actualDes);
			    matched = true;
			    // Collect result of actor activation.
			    matches.append(h);
			    break;
			}

			if (!targetSinks[k].hasVarDesignator() &&
			    targetDes != actualDes && 
			    targetDes.equals(actualDes)) {
			    matched = true;
			    // Collect result of actor activation.
			    matches.append(h);
			    break;
			}
		    }
		} else {
		    // No actors, so equivalent graphs or 
		    // valid projections means a match.
		    matched = true;
		}	
	    }
	}
    } else {			    
	String msg = "all process preconditions must be graphs.";
	throw new SemanticException(msg);		
    }
}         :
          ;

mutateKB[Type value, boolean export, Rule rule, 
	 KBase currKBase, KBase callerKBase]
{
    if (value instanceof GraphType) {
	GraphType g = (GraphType)value;
	if (g.getValue().getNumberOfConcepts() == 1 &&
	    g.getValue().getNumberOfRelations() == 0) {
	    // Do we have a context?
	    ConceptType c = new ConceptType(g.getValue().getConcepts()[0]);
	    if (!c.isContext().getValue()) {
		String msg = "process rule postconditions must be contexts.";
		throw new SemanticException(msg);		
	    }
	    // What is the context's type? Ignore its case.
	    String contextType = c.getLabel().getValue().toUpperCase();
	    // Where there is a context there is a descriptor graph, the
	    // object of our interest here.
	    GraphType desc = (GraphType)c.getDescriptor();

	    // TBD: Should actor activation be carried out for
	    // postconditions also, followed by coref var binding
	    // (the latter is already done -- see KBase.bindCorefVars())?
	    if (contextType.equals("PROPOSITION")) {
		// Assert a graph into the active KB.
		if (export || 
		    rule.isExportAllOpt() || rule.isExportAssertOpt()) {
		    currKBase.bindCorefVars(desc);
		    callerKBase.assertGraph(desc, false); // false=don't bind vars
		} else {
		    currKBase.assertGraph(desc);
		} 
	    } else if (contextType.equals("ERASURE")) {
		// Retract a graph from the active KB.
		if (export || 
		    rule.isExportAllOpt() || rule.isExportRetractOpt()) {
		    currKBase.bindCorefVars(desc);
		    callerKBase.retract(desc, false); // false=don't bind vars
		} else {
		    currKBase.retract(desc);
		} 

	    } else {
		String msg = "process postconditions must be " +
		    "contexts of type PROPOSITION or ERASURE.";
		throw new SemanticException(msg);
	    }	    
	} else {
	    String msg = "process rule postconditions must be contexts.";
	    throw new SemanticException(msg);				    
	}				    
    }
}         :
          ;

assignment
{
    Type a = UndefinedType.undefined;
    Type b = UndefinedType.undefined;
    Type c = UndefinedType.undefined;
}
	  :	#(VAR_ASSIGN id:IDENT a=expr)
		{ scopes.peek().def(id.getText(), a); }		

	  |	#(LIST_ASSIGN a=expr b=expr c=expr)
		{ a.setNthOp(b, c); } // a: list; b: index; c: value to set

	  |	#(ATTR_ASSIGN a=expr attrName:IDENT b=expr)
		{ a.setAttr(attrName.getText(), b); }
	  ;
          exception
          catch[IllegalArgumentException e] {
	      String msg = "can't apply " + e.getMessage() + 
	                   " operation to values of type " + a.getType();
	      if (b != UndefinedType.undefined) {
		  // Dyadic.
		  msg += " and " + b.getType() + "."; 
	      } else {
		  // Monadic.
		  msg += "."; 
	      }
	      throw new SemanticException(msg);
	  }
          catch[IndexOutOfBoundsException e] {
	      // List access.
	      String msg = e.getMessage();
	      throw new SemanticException(msg);
	  }	 

// ---------------------------------------------------------------------

expr returns [Type r]
{
    Type a = UndefinedType.undefined;
    r = UndefinedType.undefined;
}
          :     r=operationExpr
	  |	r=call
	  |     r=factor
          ;

operationExpr returns [Type r]
{
    LinkedList args = null; // for member function call
    Type a = UndefinedType.undefined;
    Type b = UndefinedType.undefined;
    r = UndefinedType.undefined;
}     
          :     #(OR a=expr b=expr) { r = a.orOp(b); }     
          |     #(AND a=expr b=expr) { r = a.andOp(b); }  
          |     #(GT a=expr b=expr) { r = a.gtOp(b); }   
          |     #(LT a=expr b=expr) { r = a.ltOp(b); }
          |     #(GE a=expr b=expr) { r = a.geOp(b); }      
          |     #(LE a=expr b=expr) { r = a.leOp(b); }      
          |     #(EQ a=expr b=expr) { r = a.eqOp(b); }      
          |     #(NE a=expr b=expr) { r = a.neOp(b); }      
          |     #(IS a=expr b=expr) { r = a.isOp(b); }      
          |     #(PLUS a=expr b=expr) { r = a.addOp(b); }
          |     ( #(MINUS expr expr) ) => 
                  #(MINUS a=expr b=expr) { r = a.subtractOp(b); }
          |     #(MUL a=expr b=expr) { r = a.multiplyOp(b); }
          |     #("div" a=expr b=expr) { r = a.divideOp(b); }
          |     #("mod" a=expr b=expr) { r = a.modulusOp(b); }
          |     #(MINUS a=expr) { r = a.negateOp(); }     
          |     #(NOT a=expr) { r = a.notOp(); }
	      |	    #(MEMBER_FUNCALL a=expr name:IDENT args=actualArgs)
		         { r = a.invokeMemberFunc(name.getText(), args.toArray()); }
	      |     #(LIST_SELECTION a=expr b=expr) { r = a.getNthOp(b); }
	      |     #(ATTR_SELECTION a=expr attrName:IDENT) 
		         { r = a.getAttr(attrName.getText()); }
          ;
          exception
          catch[IllegalArgumentException e] {
	      String msg = "can't apply " + e.getMessage() + 
	                   " operation to values of type " + a.getType();
	      if (b != UndefinedType.undefined) {
		  // Dyadic.
		  msg += " and " + b.getType() + "."; 
	      } else {
		  // Monadic.
		  msg += "."; 
	      }
	      throw new SemanticException(msg);
	  }
          catch[IndexOutOfBoundsException e] {
	      // List access.
	      String msg = e.getMessage();
	      throw new SemanticException(msg);
	  }	 

actualArgs returns [LinkedList list]
{
    list = new LinkedList();
    Type a = UndefinedType.undefined;
    Type b = UndefinedType.undefined;
}
	  : #( ACTUAL_ARGS ( a=expr { list.add(a); } )* )
	  ;

factor returns [Type r]
{
    LinkedList list = null;
    Type a = UndefinedType.undefined;
    Type b = UndefinedType.undefined;
    r = UndefinedType.undefined;
}         
          :     num:NUMBER
                {
		    Double d;
		    try {
			d = Double.valueOf(num.getText());
		    } catch(NumberFormatException e) {
			throw new 
			    SemanticException(num + ": illegal number format");
		    }
		    r = new NumberType(d.doubleValue());
		}

	  |	r = boolValue

          |     str:STRING
                { r = new StringType(str.getText()); }

	  |	#(LIST { list = new LinkedList(); }
			 a=expr { list.addLast(a); }
		       ( b=expr { list.addLast(b); } )*)	
		{ 
		    r = new ListType(list); 
		}

	  |	#(EMPTY_LIST { r = new ListType(); })

	  |	#(CONCEPT_LITERAL g1:GRAPH_STRING 
	        { r = new ConceptType(g1.getText()); })

	  |	#(GRAPH_LITERAL g2:GRAPH_STRING 
	        { r = new GraphType(g2.getText()); })

	  |	#(FILE_VALUE a=expr
		{
		    if (a instanceof StringType) {
			try {
			    r = new FileType(((StringType)a).getValue());
			} catch (IOException e) {
			    // r will have undefined value by default.
			}
		    } else {
			String msg = "file path must be a string.";
			throw new SemanticException(msg);
		    }
		})

	  |	r = anonFunDef

          |	#(NEW_VALUE theType:IDENT
		{
		    // New types must be in defined directory.
		    // Make this arbitrary later?
		    String typeName = theType.getText();
		    String typePath = "cgp.runtime.newtypes." + typeName;
		    try {			    
			Class c = Class.forName(typePath);
			// newInstance() requires parameterless 
			// constructor; improve. Also, check that
			// this is a Type subclass before trying
			// to cast? We'll get a ClassCastException
			// anyway.
			r = (Type)c.newInstance();
		    } catch (Exception e) {
			String msg = "cannot create an instance of " +
			             typeName;
			throw new SemanticException(msg);
		    }
		})

	  |	t:typeValue
		{ r = new StringType(t.getText()); }

	  |	id:IDENT
		{ r = scopes.find(id.getText()); } // variable

          |	#("system" a=expr { r = systemStatement(_t, a); })

                // Activate an actor or process.
		// For processes, this could also be a statement, but
		// not so for actors since a result graph is always
		// returned for them.
	  |	#("activate" a=expr 
	        { 
		    if (a instanceof GraphType) {
			GraphType g = (GraphType)a;
			if (g.containsActorNodes()) {
			    // Is this an actor or a process?
			    Type actorNode = getSingletonActorNode(_t, g);
			    if (actorNode instanceof ProcessType) {
				LinkedList args = g.getConcepts().getValue();
				r = processCall(_t, actorNode, args);
			    } else {
				// No actual arguments since we are relying
				// upon the source concepts having already
				// been bound. Note that this poses a problem
				// for anonymous recursive actors since the
				// absence of formals (no variables in 
				// designators since latter already bound)
				// makes it impossible to bind actuals by
				// referring to names. Correspondence between
				// actuals and formals therefore becomes order
				// dependent in this situation, i.e. the sink
				// concepts of the preceding executor must 
				// map by order to the source concepts of
				// the recursive actor invocation.
				r = actorCall(_t, new ActorType(g), 
					      null, false);
			    }
			} else {
			    String msg = "invalid actor/process graph.";
			    throw new SemanticException(msg);
			}
		    } else {
			String msg = "actor/process graph expected.";
			throw new SemanticException(msg);
		    }
		})
 
	  |	#("apply" a=expr b=expr { r=funApply(_t, a, b); })
          ;

// Lambda, i.e. functional programming lambda, not CG lambda, although
// they are related.
anonFunDef returns [FunctionType func]
{
    LinkedList formals = new LinkedList();
    func = null; // it *will* be initialised
}
	  :	#(ANON_FUN_DEF
		( arg:IDENT
		  { formals.add(new FormalParameter(arg.getText())); }
		)*
		b:BLOCK
		{
		    func = new FunctionType((FormalParameter[])formals.
					     toArray(new FormalParameter[0]),
					    b);
		})
	  ;

// The specified graph is a process if it contains exactly one
// actor node of type process.
//
// Note: a function such as this could be defined as a regular
// method in this class, along with the other interpreter init code
// near the top of this file. Avoid the need for passing _t/#ast.
getSingletonActorNode[GraphType g] returns [Type obj]
{
    obj = UndefinedType.undefined;

    Actor[] actorNodes = g.getValue().getActorRelations();
    
    if (actorNodes.length == 1) {
	obj = scopes.find(actorNodes[0].getType().getLabel());
    }
}
          :
          ;


boolValue returns [Type r]
{
    r = UndefinedType.undefined;	
}
	  :	"true" { r = new BooleanType(true); }
	  |	"false" { r = new BooleanType(false); }		
	  ;

typeValue :	"number" | "string" | "boolean" | "concept" | "graph" | 
		"list" | "function" | "lambda" | "actor" | "process" | 	
		"file" | "undefined"
	  ;

// ---------------------------------------------------------------------
