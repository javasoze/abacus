/*******************************************************************************************

BNF Grammar for BQL
===================

<statement> ::= ( <select_stmt> | <describe_stmt> ) [';']

<select_stmt> ::= SELECT <select_list> [<from_clause>] [<where_clause>] [<additional_clauses>]

<describe_stmt> ::= ( DESC | DESCRIBE ) [<index_name>]

<select_list> ::= '*' | (<column_name>|<aggregation_function>)( ',' <column_name>|<aggregation_function> )*

<column_name_list> ::= <column_name> ( ',' <column_name> )*

<aggregation_function> ::= <function_name> '(' <column_name> ')'

<function_name> ::= <column_name>

<from_clause> ::= FROM <index_name>

<index_name> ::= <identifier> | <quoted_string>

<where_clause> ::= WHERE <search_expr>

<search_expr> ::= <term_expr> ( OR <term_expr> )*

<term_expr> ::= <facet_expr> ( AND <facet_expr> )*

<facet_expr> ::= <predicate> 
               | '(' <search_expr> ')'

<predicates> ::= <predicate> ( AND <predicate> )*

<predicate> ::= <in_predicate>
              | <contains_all_predicate>
              | <equal_predicate>
              | <not_equal_predicate>
              | <query_predicate>
              | <between_predicate>
              | <range_predicate>
              | <match_predicate>
              | <like_predicate>
              | <null_predicate>

<in_predicate> ::= <column_name> [NOT] IN <value_list> [<except_clause>] [<predicate_props>]

<contains_all_predicate> ::= <column_name> CONTAINS ALL <value_list> [<except_clause>]
                             [<predicate_props>]

<equal_predicate> ::= <column_name> '=' <value> [<predicate_props>]

<not_equal_predicate> ::= <column_name> '<>' <value> [<predicate_props>]

<query_predicate> ::= QUERY IS <quoted_string>

<between_predicate> ::= <column_name> [NOT] BETWEEN <value> AND <value>

<range_predicate> ::= <column_name> <range_op> <numeric>

<like_predicate> ::= <column_name> [NOT] LIKE <quoted_string>

<null_predicate> ::= <column_name> IS [NOT] NULL

<value_list> ::= <non_variable_value_list> | <variable>

<non_variable_value_list> ::= '(' <value> ( ',' <value> )* ')'

<value> ::= <numeric>
          | <quoted_string>
          | TRUE
          | FALSE
          | <variable>

<range_op> ::= '<' | '<=' | '>=' | '>'

<except_clause> ::= EXCEPT <value_list>

<predicate_props> ::= WITH <prop_list>

<prop_list> ::= '(' <key_value_pair> ( ',' <key_value_pair> )* ')'

<key_value_pair> ::= <quoted_string> ':' <value>

<facet_param_list> ::= <facet_param> ( ',' <facet_param> )*

<facet_param> ::= '(' <facet_name> <facet_param_name> <facet_param_type> <facet_param_value> ')'

<facet_param_name> ::= <quoted_string>

<facet_param_type> ::= BOOLEAN | INT | LONG | STRING | BYTEARRAY | DOUBLE

<facet_param_value> ::= <quoted_string>

<additional_clauses> ::= ( <additional_clause> )+

<additional_clause> ::= <order_by_clause>
                      | <limit_clause>
                      | <distinct_clause>
                      | <browse_by_clause>
                      | <fetching_stored_clause>
                      | <explain_clause>

<order_by_clause> ::= ORDER BY <sort_specs>

<sort_specs> ::= <sort_spec> ( ',' <sort_spec> )*

<sort_spec> ::= <column_name> [<ordering_spec>]

<ordering_spec> ::= ASC | DESC

<comma_column_name_list> ::= <column_name> ( (OR | ',') <column_name> )*

<limit_clause> ::= LIMIT [<offset> ','] <count>

<offset> ::= ( <digit> )+

<count> ::= ( <digit> )+

<browse_by_clause> ::= BROWSE BY <facet_specs>

<facet_specs> ::= <facet_spec> ( ',' <facet_spec> )*

<facet_spec> ::= <facet_name> [<facet_expression>]

<facet_expression> ::= '(' <count> ')'

<expand_flag> ::= TRUE | FALSE

<facet_ordering> ::= HITS | VALUE

<fetching_stored_clause> ::= FETCHING STORED [<fetching_flag>]

<fetching_flag> ::= TRUE | FALSE

<explain_clause> ::= EXPLAIN [<explain_flag>]

<explain_flag> ::= TRUE | FALSE

<digit> ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9

<numeric> ::= <integer>
            | <real>

<integer> ::= ( <digit> )+

<real> ::= ( <digit> )+ '.' ( <digit> )+

*******************************************************************************************/

grammar BQL;

// Imaginary tokens
tokens 
{
    COLUMN_LIST,
    OR_PRED,
    AND_PRED,
    EQUAL_PRED,
    RANGE_PRED
}

// As the generated lexer will reside in com.senseidb.bql.parsers package,
// we have to add package declaration on top of it
@lexer::header {
}

@lexer::members {

  // @Override
  // public void reportError(RecognitionException e) {
  //   throw new IllegalArgumentException(e);
  // }

}

// As the generated parser will reside in bql.parsers
// package, we have to add package declaration on top of it
@parser::header {
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.text.ParseException;
import java.text.SimpleDateFormat;
}

@parser::members {

    private static enum KeyType {
      STRING_LITERAL,
      IDENT,
      STRING_LITERAL_AND_IDENT
    }
}

// ***************** parser rules:

statement
    :   ( select_stmt |   describe_stmt )   SEMI? EOF
    ;

select_stmt
    :   SELECT ('*' | cols=selection_list)
        (FROM (IDENT | STRING_LITERAL))?
        q=query_where?
        w=where?
        (   order_by = order_by_clause 
        |   limit = limit_clause
        |   browse_by = browse_by_clause
        |   fetch_stored = fetching_stored_clause
        |   explain = explain_clause
        )*
    ;

describe_stmt
    :   DESCRIBE (IDENT | STRING_LITERAL)
    ;

selection_list
    :   (   col=column_name
        |   agrFunction=aggregation_function 
        )
        (   COMMA
            (   col=column_name
            |   agrFunction=aggregation_function
            )
        )*
    ;

aggregation_function
 :   (id=function_name LPAR (columnVar=column_name | '*') RPAR)
 ;

column_name
    :   (id=IDENT | str=STRING_LITERAL)
        ('.' (id2=IDENT | str2=STRING_LITERAL)
        )*
    ;

function_name
    :   (min= 'min'|colName=column_name)
    ;

where
    :   WHERE search_expr
    ;

query_where
    :   QUERY search_expr
    ;

order_by_clause
    :   ORDER BY (RELEVANCE | sort_specs)
    ;

sort_specs
    :   sort=sort_spec
        (COMMA sort=sort_spec   // It's OK to use variable sort again here
        )*
    ;

sort_spec
    :   column_name ordering=(ASC | DESC)?
    ;

limit_clause
    :   LIMIT (n1=INTEGER COMMA)? n2=INTEGER
    ;

comma_column_name_list
    :   col=column_name
        ((OR | COMMA) col=column_name
        )*
    ;

browse_by_clause
    :   BROWSE BY f=facet_spec
        (COMMA f=facet_spec
        )*
    ;

facet_spec
    :   column_name
        (
            LPAR
            n1=INTEGER
            RPAR
        )*
    ;

fetching_stored_clause
    :   FETCHING STORED
        (   TRUE
        |   FALSE
        )*
    ;

explain_clause
    :   EXPLAIN
        (   TRUE
        |   FALSE
        )*
    ;

search_expr
    :   t=term_expr
        (OR t=term_expr)*
    ;

term_expr
    :   f=factor_expr
        (AND f=factor_expr)*
    ;

factor_expr
    :   predicate
    |   LPAR search_expr RPAR
    ;

predicate
    :   in_predicate
    |   contains_all_predicate
    |   equal_predicate
    |   not_equal_predicate
    |   query_predicate
    |   between_predicate
    |   range_predicate
    |   like_predicate
    |   null_predicate
    |   empty_predicate
    ;

in_predicate
    :   column_name not=NOT? IN value_list except=except_clause? predicate_props?
    ;

empty_predicate
    :   value_list IS (NOT)? EMPTY
    ;
    
contains_all_predicate
    :   column_name CONTAINS ALL value_list except=except_clause? predicate_props? 
    ;

equal_predicate
    :   column_name EQUAL value props=predicate_props?
    ;

not_equal_predicate
    :   column_name NOT_EQUAL value predicate_props?
    ;

query_predicate
    :   QUERY IS STRING_LITERAL
    ;

between_predicate
    :   column_name not=NOT? BETWEEN val1=value AND val2=value
    ;

range_predicate
    :   column_name op=(GT | GTE | LT | LTE) val=value
    ;

like_predicate
    :   column_name (NOT)? LIKE STRING_LITERAL
    ;

null_predicate
    :   column_name IS (NOT)? NULL
    ;

non_variable_value_list
    :   LPAR v=value
        (   COMMA v=value
        )*
        RPAR
    |   LPAR RPAR
    ;

value_list
    :   non_variable_value_list
    |   VARIABLE
    ;

value
    :   numeric
    |   STRING_LITERAL
    |   TRUE
    |   FALSE
    |   VARIABLE
    ;

numeric
    :   INTEGER
    |   REAL
    ;

except_clause
    :   EXCEPT value_list
    ;
  
predicate_props
    :   WITH prop_list[KeyType.STRING_LITERAL]
    ;

prop_list[KeyType keyType]
    :   LPAR p=key_value_pair[keyType]
        (   COMMA p=key_value_pair[keyType]
        )*
        RPAR
    ;

key_value_pair[KeyType keyType]
    :   ( { $keyType == KeyType.STRING_LITERAL ||
            $keyType == KeyType.STRING_LITERAL_AND_IDENT}? STRING_LITERAL
        | { $keyType == KeyType.IDENT ||
            $keyType == KeyType.STRING_LITERAL_AND_IDENT}? IDENT
        )
        COLON (v=value)
    ;

//
// BQL Keywords
//

ALL : [Aa][Ll][Ll] ;
AFTER : [Aa][Ff][Tt][Ee][Rr] ;
AGO : [Aa][Gg][Oo] ;
AND : [Aa][Nn][Dd] ;
AS : [Aa][Ss] ;
ASC : [Aa][Ss][Cc] ;
BEFORE : [Bb][Ee][Ff][Oo][Rr][Ee] ;
BEGIN : [Bb][Ee][Gg][Ii][Nn] ;
BETWEEN : [Bb][Ee][Tt][Ww][Ee][Ee][Nn] ;
BOOLEAN : [Bb][Oo][Oo][Ll][Ee][Aa][Nn] ;
BROWSE : [Bb][Rr][Oo][Ww][Ss][Ee] ;
BY : [Bb][Yy] ;
BYTE : [Bb][Yy][Tt][Ee] ;
BYTEARRAY : [Bb][Yy][Tt][Ee][Aa][Rr][Rr][Aa][Yy] ;
CONTAINS : [Cc][Oo][Nn][Tt][Aa][Ii][Nn][Ss] ;
DEFINED : [Dd][Ee][Ff][Ii][Nn][Ee][Dd] ;
DESC : [Dd][Ee][Ss][Cc] ;
DESCRIBE : [Dd][Ee][Ss][Cc][Rr][Ii][Bb][Ee] ;
DOUBLE : [Dd][Oo][Uu][Bb][Ll][Ee] ;
EMPTY : [Ee][Mm][Pp][Tt][Yy] ;
ELSE : [Ee][Ll][Ss][Ee] ;
END : [Ee][Nn][Dd] ;
EXCEPT : [Ee][Xx][Cc][Ee][Pp][Tt] ;
EXPLAIN : [Ee][Xx][Pp][Ll][Aa][Ii][Nn] ;
FACET : [Ff][Aa][Cc][Ee][Tt] ;
FALSE : [Ff][Aa][Ll][Ss][Ee] ;
FETCHING : [Ff][Ee][Tt][Cc][Hh][Ii][Nn][Gg] ;
FROM : [Ff][Rr][Oo][Mm] ;
HITS : [Hh][Ii][Tt][Ss] ;
IN : [Ii][Nn] ;
INT : [Ii][Nn][Tt] ;
IS : [Ii][Ss] ;
LAST : [Ll][Aa][Ss][Tt] ;
LIKE : [Ll][Ii][Kk][Ee] ;
LIMIT : [Ll][Ii][Mm][Ii][Tt] ;
LONG : [Ll][Oo][Nn][Gg] ;
MODEL : [Mm][Oo][Dd][Ee][Ll] ;
NOT : [Nn][Oo][Tt] ;
NOW : [Nn][Oo][Ww] ;
NULL : [Nn][Uu][Ll][Ll] ;
OR : [Oo][Rr] ;
ORDER : [Oo][Rr][Dd][Ee][Rr] ;
PARAM : [Pp][Aa][Rr][Aa][Mm] ;
QUERY : [Qq][Uu][Ee][Rr][Yy] ;
RELEVANCE : [Rr][Ee][Ll][Ee][Vv][Aa][Nn][Cc][Ee] ;
SELECT : [Ss][Ee][Ll][Ee][Cc][Tt] ;
SINCE : [Ss][Ii][Nn][Cc][Ee] ;
STORED : [Ss][Tt][Oo][Rr][Ee][Dd] ;
STRING : [Ss][Tt][Rr][Ii][Nn][Gg] ;
TOP : [Tt][Oo][Pp] ;
TRUE : [Tt][Rr][Uu][Ee] ;
USING : [Uu][Ss][Ii][Nn][Gg] ;
VALUE : [Vv][Aa][Ll][Uu][Ee] ;
WHERE : [Ww][Hh][Ee][Rr][Ee] ;
WITH : [Ww][Ii][Tt][Hh] ;

fragment DIGIT : '0'..'9' ;
fragment ALPHA : 'a'..'z' | 'A'..'Z' ;
fragment INTEGER_TYPE_SUFFIX: ('l' | 'L') ;

INTEGER : ('0' | '1'..'9' '0'..'9'*) INTEGER_TYPE_SUFFIX? ;
REAL : DIGIT+ '.' DIGIT* ;
LPAR : '(' ;
RPAR : ')' ;
COMMA : ',' ;
COLON : ':' ;
SEMI : ';' ;
EQUAL : '=' ;
GT : '>' ;
GTE : '>=' ;
LT : '<' ;
LTE : '<=';
NOT_EQUAL : '<>' ;
DOT : '.';
LBRACE : '{';
RBRACE : '}';
LBRACK : '[';
RBRACK : ']';
PLUS : '+';
MINUS : '-';
STAR : '*';
DIV : '/';
MOD : '%';
INC : '++';
DEC : '--';
TILDE : '~';
BANG : '!';
CARET : '^';
EQEQ : '==';
NEQ : '!=';
PLUSEQ : '+=';
MINUSEQ : '-=';
STAREQ : '*=';
DIVEQ : '/=';
AMPEQ : '&=';
PIPEEQ : '|=';
CARETEQ : '^=';
MODEQ : '%=';
OROR : '||';
ANDAND : '&&';
PIPE : '|';
AMP : '&';
QUES : '?';

STRING_LITERAL
    :   '"'
        (   '"' '"'
        |   ~('"'|'\r'|'\n')
        )*
        '"'
    |   '\''
        (   '\'' '\''
        |   ~('\''|'\r'|'\n')
        )*
        '\''
    ;

// Have to define this after the keywords?
IDENT : (ALPHA | '_') (ALPHA | DIGIT | '-' | '_')* ;
VARIABLE : '$' (ALPHA | DIGIT | '_')+ ;

WS : ( ' ' | '\t' | '\r' | '\n' )+ -> channel(HIDDEN);

COMMENT
    : '/*' .*? '*/' -> channel(HIDDEN)
    ;

LINE_COMMENT
    : '--' ~('\n'|'\r')* -> channel(HIDDEN)
    ;
