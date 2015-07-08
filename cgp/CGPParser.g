header {
    package cgp;
}

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
 * Conceptual Graph Processes parser.
 *
 * David Benn, June-October 2000
 */

class CGPParser extends Parser;

options {
    exportVocab=CGP;
    buildAST=true;
    defaultErrorHandler=false;
}

imaginaryTokenDefinitions
          :
    OPTION_LIST
    TYPE_DECL
    VALUE_SELECTOR
    BLOCK
    IF_STATEMENT
    WHILE_STATEMENT
    FOREACH_STATEMENT
    CONDITION
    FUN_DEF
    ANON_FUN_DEF
    LAMBDA_DEF
    ACTOR_DEF
    PROCESS_DEF
    RULE_DEF
    PRE_DEF
    MATCH_EXPRESSION
    MUTATE_KB_EXPRESSION
    POST_DEF
    CALL
    ACTUAL_ARGS
    VAR_ASSIGN
    ATTR_ASSIGN
    LIST_ASSIGN
    MEMBER_FUNCALL
    ATTR_SELECTION
    LIST_SELECTION
    LIST
    CONCEPT_LITERAL
    GRAPH_LITERAL
    FILE_VALUE
    NEW_VALUE
    EMPTY_LIST
    TYPE_VALUE
    AND OR IS NOT	// letter-only operators
          ;

// ---------------------------------------------------------------------

program   :     ( topLevelStatement )*
          ;

// ---------------------------------------------------------------------

// ** Statement productions **

// All statements make sense at the top-level.
topLevelStatement
          :     ( nodeKind IDENT ) => typeDecl // conflict with typeValue
          |     ( "function" IDENT ) => funDef // conflict with typeValue
          |     ( "lambda" IDENT ) => lambdaDef // conflict with typeValue
          |     ( "actor" IDENT ) => actorDef // conflict with typeValue       
          |     ( "process" IDENT ) => processDef // conflict with typeValue  
          |     optionList
          |     statement
          ;

typeDecl  :	nodeKind IDENT ( GT! IDENT )* SEMI!
		{ #typeDecl = #(#[TYPE_DECL], #typeDecl); }
	  ;

nodeKind  :     "concept" | "relation"
          ;

funDef	  :	"function"! IDENT 
		LPAREN! ( IDENT )? ( COMMA! IDENT )* RPAREN! block
		{ #funDef = #(#[FUN_DEF], #funDef); }
	  ;

actorDef  :	"actor"! IDENT
                LPAREN! IDENT 
                        ( COMMA! IDENT )* RPAREN! 
                "is"! expr
		{ #actorDef = #(#[ACTOR_DEF], #actorDef); }
	  ;

actorParamKind 
          :     "in" | "out"
          ;

lambdaDef  
	  :	"lambda"! IDENT
                LPAREN! IDENT 
                        ( COMMA! IDENT )* RPAREN! 
                "is"! expr
		{ #lambdaDef = #(#[LAMBDA_DEF], #lambdaDef); }
	  ;

block	  :	( statement )* "end"!
		{ #block = #(#[BLOCK], #block); }
	  ;

processDef:     "process"! IDENT
                LPAREN! ( processParamKind IDENT )?
                        ( COMMA! processParamKind IDENT )* RPAREN! 
                ( "initial"! block )? 
		( ruleDef )*
                "end"!
		{ #processDef = #(#[PROCESS_DEF], #processDef); }
          ;

processParamKind 
          :     "in" | "out"
          ;

ruleDef   :     "rule"! IDENT 
		( optionList )?
               	preDef postDef "end"!
                { #ruleDef = #(#[RULE_DEF], #ruleDef); } 
          ;

preDef    :     "pre"! 	( "action"! block )? 
			( ( TILDE )? matchExpression )* 
		"end"!
                { #preDef = #(#[PRE_DEF], #preDef); }
          ;

matchExpression
	  :	expr SEMI!
		{ #matchExpression = #(#[MATCH_EXPRESSION], 
				     #matchExpression); }
	  ;

postDef   :     "post"! ( "action"! block )? 
			( mutateKBExpression ( optionList )? )* 
		"end"!
                { #postDef = #(#[POST_DEF], #postDef); }
          ;

mutateKBExpression
	  :	expr SEMI!
		{ #mutateKBExpression = #(#[MUTATE_KB_EXPRESSION], 
				        #mutateKBExpression); }
	  ;

optionList:	"option"! IDENT (GETS! STRING)?
                      ( COMMA! IDENT (GETS! STRING)?)* SEMI!
		{ #optionList = #(#[OPTION_LIST], #optionList); }
	  ;

statement :	intrinsicStatement
	  |     ifStatement
          |     whileStatement
          |     foreachStatement
	  |	( factor DOT IDENT actualArgs ) => memberFunCall SEMI!	
          |     ( factor actualArgs ) => call SEMI!
          |     assignment
	  |	SEMI! // the empty statement, e.g. if ... then ... else; end
          ;

intrinsicStatement
	  :     "print"^ expr SEMI!
          |     "println"^ expr SEMI! // or recognise \n \t etc -- see J0
 		// exit functions and processes with a value
          |     ( "return" expr ) => "return"^ expr SEMI!
		// exit functions and processes without a value
          |     "return"^ SEMI!
 		// terminates program with exit value
          |     ( "exit" expr ) => "exit"^ expr SEMI!
		// terminates program without exit value
          |     "exit"^ SEMI! 
	  |	"last"^ SEMI! // break out of a loop
	  |	"system"^ expr SEMI! // executes an operating system command
          |     "assert"^ expr SEMI! // assert a graph in the active KB
          |     "retract"^ expr SEMI! // retract a graph from the active KB
	  |	"apply"^ expr expr SEMI!
	  ;

ifStatement
          : 	"if"! condition "then"! block 
                ( "else"! block )?
		{ #ifStatement = #(#[IF_STATEMENT], #ifStatement); }
	  ;

whileStatement
	  : 	"while"! condition "do"! block
		{ #whileStatement = #(#[WHILE_STATEMENT], #whileStatement); }
          ;

// This production is necessary when evaluation must be delayed/repeated 
// as in a loop.
condition : 	expr
		{ #condition = #(#[CONDITION], #condition); }
	  ;

foreachStatement
	  : 	"foreach"! IDENT "in"! expr "do"! block
		{ #foreachStatement = #(#[FOREACH_STATEMENT], 
                                        #foreachStatement); }
          ;

call      :	factor actualArgs
		{ #call = #(#[CALL], #call); }
	  ;

memberFunCall	
	  :	factor DOT! IDENT actualArgs
	   	{ #memberFunCall = #(#[MEMBER_FUNCALL], #memberFunCall); }
	  ;

// The two optional expression components can be combined when
// walking the generated abstract syntax tree, i.e. (x)? (x)* = (x)*
actualArgs:	LPAREN! ( expr )? ( COMMA! expr )* RPAREN!
		{ #actualArgs = #(#[ACTUAL_ARGS], #actualArgs); }
	  ;

assignment:	(IDENT GETS) => IDENT GETS! expr SEMI!
		{ #assignment = #(#[VAR_ASSIGN], #assignment); }

	  |	(factor LBRACK) => factor LBRACK! expr RBRACK! GETS! expr SEMI!
	        { #assignment = #(#[LIST_ASSIGN], #assignment); }

	  |	(factor DOT) => factor DOT! IDENT GETS! expr SEMI!
	        { #assignment = #(#[ATTR_ASSIGN], #assignment); }
	  ;

// ---------------------------------------------------------------------

/** Expression productions **
  *
  * Precedence of operators (from lowest to highest)
  * -----------------------				Associativity
  *							-------------
  *	or							L	
  *	and							L
  *	> < >= <= == !=	is (is: type equivalence operator)	L
  *	+ -							L
  *	* div mod						L
  *	- not (unary negation and logical complement)		L
  *	[] . (list indexing and attribute/method selection operators)
  *
  *	Notes:  - The above prec/assoc table (except for rel ops) is a subset 
  *		  of Java's but some names have changed for readability's sake.
  *		- and & or are NOT short circuit operators.
  *
  */

expr 	  :	andExpr ( "or"! andExpr { #expr = #(#[OR], #expr); } )*
	  ;

andExpr   :	relExpr ( "and"! relExpr { #andExpr = #(#[AND], #andExpr); } )*
	  ;

relExpr	  : 	addExpr 
		( 
		// Relational operators. EQ also used for type checking?
		 ( GT^ addExpr )
		|( LT^ addExpr )
		|( GE^ addExpr )
		|( LE^ addExpr )
		|( EQ^ addExpr ) // move EQ and NE to own production ala Java?
		|( NE^ addExpr )
		|("is"! addExpr) { #relExpr = #(#[IS], #relExpr); }
		)*
	  ;

addExpr   :     mulExpr
		( 
		 options {
	             // This has been added to supress a strange warning
		     // when "apply" was added to the intrinsicStatement 
		     // rule. The error related to alterative 2 (MINUS
		     // presumably) and the exit branch of *this* block!
		     // The only difference between apply and other alts
		     // in intrinsicStatement is that apply take two expr
		     // arguments.
		     warnWhenFollowAmbig=false;
	  	 }:
		 ( PLUS^ mulExpr )
		|( MINUS^ mulExpr )
		)*
          ;

mulExpr   :     unaryExpr
		(
		 ( MUL^ unaryExpr )
		|( "div"^ unaryExpr )
		|( "mod"^ unaryExpr )
		)*
          ;

unaryExpr :	MINUS^ selectionExpr
	  |	"not"! selectionExpr
		  { #unaryExpr = #(#[NOT], #unaryExpr); }
	  |	selectionExpr
	  ;

selectionExpr
	  :	callExpr
		(
		 options {
		     warnWhenFollowAmbig=false;
	  	 }
		// Member function call.
	        :( DOT IDENT LPAREN ) => DOT! IDENT actualArgs
		 { #selectionExpr = #(#[MEMBER_FUNCALL], #selectionExpr); }

		// Attribute access.
	        |( DOT! IDENT )
		 { #selectionExpr = #(#[ATTR_SELECTION], #selectionExpr); }

		// List access.
		|( LBRACK! expr RBRACK! )
                 { #selectionExpr = #(#[LIST_SELECTION], #selectionExpr); }
		)?
	  ;

callExpr  :	( "system" factor ) => "system"^ factor
	  |	( "activate" factor ) => "activate"^ factor
	  |	( "apply" factor ) => "apply"^ factor factor
	  |	( factor actualArgs ) => call
	  | 	( factor ) => factor
	  ;

factor    : 	NUMBER
          |     STRING
          |     boolValue
          |     listValue
          |     graphValue
          |     ( "concept" GRAPH_STRING ) => conceptValue 
	      |	    ( "file" factor ) => fileValue	
	      |	    ( "function" LPAREN ) => anonFunDef
	      |	    ( "new" IDENT ) => newValue
		  |	    typeValue
	      |	    IDENT // variable
          |     LPAREN! expr RPAREN!
          ;

boolValue :     "true" | "false"
          ;

// It would be better to treat these as lexer literals and pass a token and
// its text back to the parser. Also, for the sake of extensibility of the
// type system, it would be better to treat these as simple strings in 
// conjunction with "is" operator, albeit not as pretty since quotes are
// required. 
typeValue :	"number" | "string" | "boolean" | "concept" | "graph" | 
		"list" | "function" | "lambda" | "actor" | "process" | 	
		"file" | "undefined"
	  ;

listValue :     ( LBRACE! RBRACE! ) => LBRACE! RBRACE! // the empty list
                { #listValue = #(#[EMPTY_LIST], #listValue); }
	  |	LBRACE! expr ( COMMA! expr )* RBRACE!
                { #listValue = #(#[LIST], #listValue); }
	  ;

conceptValue
	  :     "concept"! GRAPH_STRING  // pre-parse as per ANSI standard?
		{ #conceptValue = #(#[CONCEPT_LITERAL], #conceptValue); }
          ;

graphValue  
	  :     GRAPH_STRING
		{ #graphValue = #(#[GRAPH_LITERAL], #graphValue); }
	  ;

fileValue :	"file"! factor
		{ #fileValue = #(#[FILE_VALUE], #fileValue); }
	  ;

anonFunDef:	"function"!
		LPAREN! ( IDENT )? ( COMMA! IDENT )* RPAREN! block
		{ #anonFunDef = #(#[ANON_FUN_DEF], #anonFunDef); }
	  ;

newValue  :	"new"! factor
		{ #newValue = #(#[NEW_VALUE], #newValue); }
	  ;

// -------------------------------------------------------------------

/** 
 * Conceptual Graph Processes lexical analyser.
 *
 * David Benn, June-October 2000
 */

class CGPLexer extends Lexer;

options {
    k=2;
    exportVocab=CGP;
    // Use general ASCII character set -- see ANTLR docs
    charVocabulary = '\3'..'\377';
}

// TBD: add tokens list? -- see tinyc example lexer

LPAREN  :       '(' 
        ;

RPAREN  :       ')' 
        ;

LBRACK  :       '[' 
        ;

RBRACK  :       ']' 
        ;

LBRACE  :       '{' 
        ;

RBRACE  :       '}' 
        ;

SEMI    :       ';'  
        ;

COLON   :       ':'  // seems to be needed for LF graph literals to parse
	    ;            // (should be able to remove this due to charVocabulary).

DOT     :       '.' 
        ;

COMMA   :       ','
        ;

GETS	:	'='
	;

PLUS	:	'+'  
        ;

MINUS	:	'-' 
        ;

MUL	:	'*'  
        ;

EQ	:	"==" 
        ;

NE	:	"!=" 
        ;

GT	:	'>' 
        ;

LT	:	'<' 
        ;

GE	:	">=" 
        ;

LE	:	"<=" 
        ;

TILDE	:	'~'
	;

IDENT   :   ( LETTER | '_' ) ( DIGIT | LETTER | '_' )*
        ;

NUMBER	:	( DIGIT )+ ( '.' (DIGIT)+ )?	// simple real number
	;

protected
LETTER  :       'A'..'Z' | 'a'..'z' 
	;

protected
DIGIT   :       '0'..'9' 
	;

protected
METACHAR:	'?' | '|' | '\'' | '#' | '+' | '*' 
	| 	'(' | ')' | '{' | '}' | '~'
	;

STRING  :	'"'! (~'"')* '"'!
	;

GRAPH_STRING
	:	'`'! (~'`')+ '`'!
	;

WS      :	((("\r\n" | '\r' | '\n') { newline(); })
	|	' '
	|	'\t' 
	|	'\f')   
                { _ttype = Token.SKIP; }   
	;

/* Single and multi-line comments terminated by DOS/Mac/Unix end-of-line 
 * sequence but commented via a block Java comment. :)
 *
 * Note that both Java/C++ and shell-style comments are permitted. The
 * latter makes it possible to have an interpreter specification line
 * in Unix shells. 
 */
COMMENT 
   	:	"//" (~('\r' | '\n'))* ("\r\n" | '\r' | '\n')
		{ newline(); _ttype = Token.SKIP; }

	|	'#' (~('\r' | '\n'))* ("\r\n" | '\r' | '\n')
		{ newline(); _ttype = Token.SKIP; }
	|	"/*"
		(options { // necessary because of LA(n)?
		     warnWhenFollowAmbig=false;
	  	 }:	{ LA(2)!='/' }? '*'
		|	("\r\n") => ("\r\n") { newline(); }
		|	('\r' | '\n') { newline(); }
		|	~('*'| '\r' | '\n')
		)*
		"*/"
		{ _ttype = Token.SKIP; }
	;
