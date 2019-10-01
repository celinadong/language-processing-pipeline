
// File:   MH_Typechecker.java

// Java template file for typechecker component of Informatics 2A Assignment 1.
// Provides infrastructure for Micro-Haskell typechecking:
// the core typechecking operation is to be implemented by students.

import java.util.*;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.BooleanType;

import java.io.*;

class MH_Typechecker {

	static MH_Parser MH_Parser1 = MH_Type_Impl.MH_Parser1;

	// The core of the typechecker:
	// Computing the MH_TYPE of a given MH_EXP in a given TYPE_ENV.
	// Should raise TypeError if MH_EXP isn't well-typed

	static MH_TYPE IntegerType = MH_Type_Impl.IntegerType;
	static MH_TYPE BoolType = MH_Type_Impl.BoolType;

	static MH_TYPE computeType(MH_EXP exp, TYPE_ENV env) throws TypeError, UnknownVariable {

		if (exp.isVAR()) {
			return env.typeOf(exp.value());
		}
		else if (exp.isNUM()) {
			String numeric = exp.value();
			int numLength = String.valueOf(numeric).length();
			for (int i = 0; i < numLength; i++) {
				char[] numDigits = numeric.toCharArray(); // convert the numeric expression into a character array to iterate
													// through each digit
				if (numDigits[i] < '0' || numDigits[i] > '9') {
					throw new TypeError(
							"Expression type error: non-digit character " + numDigits[i] + " found in " + numeric);
				}
			}
			return IntegerType;
		}
		else if (exp.isBOOLEAN()) {
			String bool = exp.value();
			if (bool != "True" && bool != "False") {
				throw new TypeError("Expression type error: non-boolean expression");
			}
			return BoolType;
		}
		else if (exp.isAPP()) {
			MH_TYPE a = computeType(exp.first(), env);
			MH_TYPE b = computeType(exp.second(), env);

			// type of b has to match argument type of a
			if (a.left().equals(b)) {
				return a.right();
			}
			else {
				throw new TypeError("Expression type error: type of the second expression does not match argument type of first expression");
			}
		}
		else if (exp.isINFIX()) {
			String infix = exp.infixOp();
			MH_TYPE a = computeType(exp.first(), env);
			MH_TYPE b = computeType(exp.second(), env);

			// type of both a and b has to be the same
			if (a.isInteger() && b.isInteger()) {
				if (infix.equals("==") || infix.equals("<=")) {
					return BoolType;
				}
				else if (infix.equals("+") || infix.equals("-")) {
					return IntegerType;
				}
				else {
					throw new TypeError("Expression type error: infix operator " + infix + " is not in the language");
				}
			}
			else {
				throw new TypeError("Expression type error: argument are not of type Integer");
			}
		}
		else if (exp.isIF()) {
			MH_TYPE a = computeType(exp.first(), env);
			MH_TYPE b = computeType(exp.second(), env);
			MH_TYPE c = computeType(exp.third(), env);

			if (a.isBool()) {
				if (b.equals(c)) {
					return c;
				}
				else {
					throw new TypeError("Expression type error: types for the second and third expression do not match");
				}
			}
			else {
				throw new TypeError("Expression type error: type of first expression is not boolean");
			}
		}
		else {
			throw new TypeError("Expression is incorrect");
		}
	}

	// Type environments:

	interface TYPE_ENV {
		MH_TYPE typeOf(String var) throws UnknownVariable;
	}

	static class MH_Type_Env implements TYPE_ENV {

		TreeMap<String, MH_TYPE> env;

		public MH_TYPE typeOf(String var) throws UnknownVariable {
			MH_TYPE t = (MH_TYPE) (env.get(var));
			if (t == null)
				throw new UnknownVariable(var);
			else
				return t;
		}

		// Constructor for cloning a type env
		MH_Type_Env(MH_Type_Env given) {
			this.env = new TreeMap<String, MH_TYPE>();
			this.env.putAll(given.env);
			// Old version (causes unchecked typecast warning):
			// this.env = (TreeMap<String,MH_TYPE>)given.env.clone() ;
		}

		// Constructor for building a type env from the type decls
		// appearing in a program
		MH_Type_Env(TREE prog) throws DuplicatedVariable {
			this.env = new TreeMap<String, MH_TYPE>();
			TREE prog1 = prog;
			while (prog1.getRhs() != MH_Parser.epsilon) {
				TREE typeDecl = prog1.getChildren()[0].getChildren()[0];
				String var = typeDecl.getChildren()[0].getValue();
				MH_TYPE theType = MH_Type_Impl.convertType(typeDecl.getChildren()[2]);
				if (env.containsKey(var))
					throw new DuplicatedVariable(var);
				else
					env.put(var, theType);
				prog1 = prog1.getChildren()[1];
			}
			System.out.println("Type conversions successful.");
		}

		// Augmenting a type env with a list of function arguments.
		// Takes the type of the function, returns the result type.
		MH_TYPE addArgBindings(TREE args, MH_TYPE theType) throws DuplicatedVariable, TypeError {
			TREE args1 = args;
			MH_TYPE theType1 = theType;
			while (args1.getRhs() != MH_Parser.epsilon) {
				if (theType1.isFun()) {
					String var = args1.getChildren()[0].getValue();
					if (env.containsKey(var)) {
						throw new DuplicatedVariable(var);
					} else {
						this.env.put(var, theType1.left());
						theType1 = theType1.right();
						args1 = args1.getChildren()[1];
					}
				} else
					throw new TypeError("Too many function arguments");
			}
			;
			return theType1;
		}
	}

	static MH_Type_Env compileTypeEnv(TREE prog) throws DuplicatedVariable {
		return new MH_Type_Env(prog);
	}

	// Building a closure (using lambda) from argument list and body
	static MH_EXP buildClosure(TREE args, MH_EXP exp) {
		if (args.getRhs() == MH_Parser.epsilon)
			return exp;
		else {
			MH_EXP exp1 = buildClosure(args.getChildren()[1], exp);
			String var = args.getChildren()[0].getValue();
			return new MH_Exp_Impl(var, exp1);
		}
	}

	// Name-closure pairs (result of processing a TermDecl).
	static class Named_MH_EXP {
		String name;
		MH_EXP exp;

		Named_MH_EXP(String name, MH_EXP exp) {
			this.name = name;
			this.exp = exp;
		}
	}

	static Named_MH_EXP typecheckDecl(TREE decl, MH_Type_Env env)
			throws TypeError, UnknownVariable, DuplicatedVariable, NameMismatchError {
		// typechecks the given decl against the env,
		// and returns a name-closure pair for the entity declared.
		String theVar = decl.getChildren()[0].getChildren()[0].getValue();
		String theVar1 = decl.getChildren()[1].getChildren()[0].getValue();
		if (!theVar.equals(theVar1))
			throw new NameMismatchError(theVar, theVar1);
		MH_TYPE theType = MH_Type_Impl.convertType(decl.getChildren()[0].getChildren()[2]);
		MH_EXP theExp = MH_Exp_Impl.convertExp(decl.getChildren()[1].getChildren()[3]);
		TREE theArgs = decl.getChildren()[1].getChildren()[1];
		MH_Type_Env theEnv = new MH_Type_Env(env);
		MH_TYPE resultType = theEnv.addArgBindings(theArgs, theType);
		MH_TYPE expType = computeType(theExp, theEnv);
		if (expType.equals(resultType)) {
			return new Named_MH_EXP(theVar, buildClosure(theArgs, theExp));
		} else
			throw new TypeError("RHS of declaration of " + theVar + " has wrong type");
	}

	static MH_Exp_Env typecheckProg(TREE prog, MH_Type_Env env)
			throws TypeError, UnknownVariable, DuplicatedVariable, NameMismatchError {
		TREE prog1 = prog;
		TreeMap<String, MH_EXP> treeMap = new TreeMap<String, MH_EXP>();
		while (prog1.getRhs() != MH_Parser.epsilon) {
			TREE theDecl = prog1.getChildren()[0];
			Named_MH_EXP binding = typecheckDecl(theDecl, env);
			treeMap.put(binding.name, binding.exp);
			prog1 = prog1.getChildren()[1];
		}
		System.out.println("Typecheck successful.");
		return new MH_Exp_Env(treeMap);
	}

	// For testing:

	public static void main(String[] args) throws Exception {
		Reader reader = new BufferedReader(new FileReader(args[0]));
		// try {
		LEX_TOKEN_STREAM MH_Lexer = new CheckedSymbolLexer(new MH_Lexer(reader));
		TREE prog = MH_Parser1.parseTokenStream(MH_Lexer);
		MH_Type_Env typeEnv = compileTypeEnv(prog);
		MH_Exp_Env runEnv = typecheckProg(prog, typeEnv);
		// } catch (Exception x) {
		// System.out.println ("MH Error: " + x.getMessage()) ;
		// }
	}
}