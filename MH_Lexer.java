
// File:   MH_Lexer.java

// Java template file for lexer component of Informatics 2A Assignment 1.
// Concerns lexical classes and lexer for the language MH (`Micro-Haskell').


import java.io.* ;

class MH_Lexer extends GenLexer implements LEX_TOKEN_STREAM {

	static class VarAcceptor extends Acceptor implements DFA {
		public String lexClass() {return "VAR"; };
		public int numberOfStates() {return 3; };
        // small (small + large + digit + ')*
        // Always starts with a small character, followed by nothing,
        // or any number of repetitions of the characters affected by Kleene star.
		int next (int state, char c) {
			switch (state) {
			case 0: if (CharTypes.isSmall(c)) return 1; else return 2;
			case 1: if (CharTypes.isSmall(c) || CharTypes.isLarge(c) || CharTypes.isDigit(c) || (c == '\'')) return 1; else return 2;
            default: return 2;
			}
		}
		boolean accepting (int state) {return (state == 1); }
		int dead () {return 2; }
	}

	static class NumAcceptor extends Acceptor implements DFA {
		public String lexClass() {return "NUM"; };
		public int numberOfStates() {return 4; };
        // 0 + nonZeroDigit digit *
        // Either a zero, or a nonzero digit followed by any nonzero digits
		int next (int state, char c) {
			switch (state) {
            case 0: if (c == '0') return 1; else if ((c != 0) && (CharTypes.isDigit(c))) return 2; else return 3;
            case 1: return 3;
			case 2: if (CharTypes.isDigit(c)) return 2; else return 3;
			default: return 3;
			}
		}
		boolean accepting (int state) {return (state == 1 || state == 2); }
		int dead ()  {return 3; }
	}

	static class BooleanAcceptor extends Acceptor implements DFA {
		public String lexClass() { return "BOOLEAN"; };
		public int numberOfStates() {return 9; };
        // True + False
        // Accepts one instance of either True or False, not both.
		int next (int state, char c) {
			switch (state) {
			case 0: if (c == 'T') return 1; else if (c == 'F') return 5; else return 8;
			case 1: if (c == 'r') return 2; else return 8;
			case 2: if (c == 'u') return 3; else return 8; 
            case 3: if (c == 'e') return 4; else return 8;
            case 4: return 8;
			case 5: if (c == 'a') return 6; else return 8;
			case 6: if (c == 'l') return 7; else return 8;
			case 7: if (c == 's') return 3; else return 8;
			default: return 8;
			}
		}
		boolean accepting (int state) {return (state == 4); }
		int dead () {return 8; }
	}

	static class SymAcceptor extends Acceptor implements DFA {
		public String lexClass() { return "SYM"; };
		public int numberOfStates() {return 3; };
        // symbolic symbolic*
        // Any number of repetitions of a symbolic character.
		int next (int state, char c) {
			switch (state) {
            case 0: if (CharTypes.isSymbolic(c)) return 1; else return 2;
            case 1: if (CharTypes.isSymbolic(c)) return 1; else return 2;
			default: return 2;
			}
		}
		boolean accepting (int state) {return (state == 1); }
		int dead () {return 2; }
	}

	static class WhitespaceAcceptor extends Acceptor implements DFA {
		public String lexClass() {return ""; };
		public int numberOfStates() {return 3; };
        // whitespace whitespace*
        // Any amount of repetitions of whitespace characters.
		int next (int state, char c) {
			switch (state) {
            case 0: if (CharTypes.isWhitespace(c)) return 1; else return 2;
            case 1: if (CharTypes.isWhitespace(c)) return 1; else return 2;
			default: return 2;
			}
		}
		boolean accepting (int state) {return (state == 1); }
		int dead () {return 2; }
	}

	static class CommentAcceptor extends Acceptor implements DFA {
		public String lexClass() {return ""; };
		public int numberOfStates() {return 6; };
        // - - -* (nonSymbolNewline nonNewline* + ε)
        // Always two dashes, followed by any number of dashes, and either nothing or
        // a non-symbol or non-newline character followed by any number of non-newline characters.
		int next (int state, char c) {
			switch (state) {
			case 0: if (c == '-') return 1; else return 5;
			case 1: if (c == '-') return 2; else return 5;
			case 2: if (c == '-') return 2; else if (!(CharTypes.isSymbolic(c)) && !(CharTypes.isNewline(c))) return 3; else return 5;
			case 3: if (!(CharTypes.isNewline(c))) return 3; else if (CharTypes.isNewline(c)) return 4; else return 5;
            default: return 5;
			}
		}
		boolean accepting (int state) { return (state == 2 || state == 3 || state == 4); }
		int dead () {return 5; }
	}

	static class TokAcceptor extends Acceptor implements DFA {

		String tok ;
		int tokLen ;
		
		TokAcceptor (String tok) {
            this.tok = tok;
            tokLen = tok.length();
        }

		public String lexClass() {return tok; };
		public int numberOfStates() {return tokLen + 2; }; // for a string of length n, there are n + 1 states. Thus, if we add the garbage state there are n + 2 states in total
		
		int next (int state, char c) {
            if (state < tokLen) {
                if (tok.charAt(state) == c) {
                    state += 1;
                    return state;
                }
                else {
                    return tokLen + 1; // else return garbage state
                }
            }
            else {
                return tokLen + 1;
            }
		}

		boolean accepting (int state) { return (state == tokLen) ; }
		int dead () {return (tokLen + 1); } // garbage state is always going to be the length of the string plus 1
	}

	static DFA variableLit = new VarAcceptor();
	static DFA numericLit = new NumAcceptor();
	static DFA booleanLit = new BooleanAcceptor();
	static DFA symbolLit = new SymAcceptor();
	static DFA whitespace = new WhitespaceAcceptor();
	static DFA comments = new CommentAcceptor();
	static DFA intKeyword = new TokAcceptor("Integer");
	static DFA boolKeyword = new TokAcceptor("Bool");
	static DFA ifKeyword = new TokAcceptor("if");
	static DFA thenKeyword = new TokAcceptor("then");
	static DFA elseKeyword = new TokAcceptor("else");
	static DFA openParenthesis = new TokAcceptor("(");
	static DFA closeParenthesis = new TokAcceptor(")");
	static DFA semicolon = new TokAcceptor(";");
	
	static DFA[] MH_acceptors = 
			new DFA[] {intKeyword, boolKeyword, ifKeyword, thenKeyword, elseKeyword, 
                openParenthesis, closeParenthesis, semicolon, variableLit, whitespace,
                comments, symbolLit, numericLit, booleanLit};
	
	MH_Lexer (Reader reader) {
		super(reader,MH_acceptors);
	}

}


/*
34/35
Line 235
Good code that shows good understanding of DFAs. In the commentAcceptor class,
in state 3, if we get a newline character, we should go to a reject state.                 

Autotesting DFAs:

Testing VarAcceptor on input "x"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing VarAcceptor on input "x23Y"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing VarAcceptor on input "xc'f"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing VarAcceptor on input ""
  Should reject in a live state.   TEST PASSED: 1 mark.
Testing VarAcceptor on input "x$Bc"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing VarAcceptor on input "X"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing NumAcceptor on input "0"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing NumAcceptor on input "23"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing NumAcceptor on input "04"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing NumAcceptor on input "x"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing BooleanAcceptor on input "True"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing BooleanAcceptor on input "False"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing BooleanAcceptor on input "Tru"
  Should reject in a live state.   TEST PASSED: 1 mark.
Testing BooleanAcceptor on input "true"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing BooleanAcceptor on input "Truee"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing SymAcceptor on input "+*"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing SymAcceptor on input "5"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing WhitespaceAcceptor on input "\t "
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing WhitespaceAcceptor on input ""
  Should reject in a live state.   TEST PASSED: 1 mark.
Testing WhitespaceAcceptor on input " 4"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing CommentAcceptor on input "--"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing CommentAcceptor on input "-- comment"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing CommentAcceptor on input "--A+*("
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing CommentAcceptor on input "--+"
  Should reject in a dead state.   TEST PASSED: 1 mark.
Testing CommentAcceptor on input "---- ABCD\n"
  Should reject in a dead state.   TEST FAILED: 0 marks
Testing TokAcceptor("test") on input "test"
  Should accept in a live state.   TEST PASSED: 1 mark.
Testing TokAcceptor("test") on input "tes"
  Should reject in a live state.   TEST PASSED: 1 mark.
Testing TokAcceptor("test") on input "tests"
  Should reject in a dead state.   TEST PASSED: 1 mark.


Autotesting lexer:

Testing lexer on input "else++".  Tokens should be :
   "else" of class "else"
   "++" of class "SYM"
   TEST PASSED: 1 mark.
Testing lexer on input "else1+".  Tokens should be :
   "else1" of class "VAR"
   "+" of class "SYM"
   TEST PASSED: 1 mark.
Testing lexer on input "()".  Tokens should be :
   "(" of class "("
   ")" of class ")"
   TEST PASSED: 1 mark.
Testing lexer on input "---+ ".  Tokens should be :
   "---+" of class "SYM"
   " " of class ""
   TEST PASSED: 1 mark.
Testing lexer on input "--  ".  Tokens should be :
   "--  " of class ""
   TEST PASSED: 1 mark.
Testing lexer on input "--".  Tokens should be :
   "--" of class ""
   TEST PASSED: 1 mark.
Testing lexer on input "+13fy".  Tokens should be :
   "+" of class "SYM"
   "13" of class "NUM"
   "fy" of class "VAR"
   TEST PASSED: 1 mark.
*/
