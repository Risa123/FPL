package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.block.ConditionalBlock;
import risa.fpl.function.block.ForLoop;
import risa.fpl.function.block.TryCatchFinally;
import risa.fpl.function.statement.Break;
import risa.fpl.function.statement.Return;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;

public class FnEnv extends FnSubEnv{
	private static final Return RETURN = new Return();
	private static final ConditionalBlock IF = new ConditionalBlock("if");
	private static final ConditionalBlock WHILE = new ConditionalBlock("while");
	private static final Break BREAK = new Break();
	private static final TryCatchFinally TRY_CATCH_FINALLY = new TryCatchFinally();
	private static final ForLoop FOR = new ForLoop();
	private final TypeInfo returnType;
	public FnEnv(AEnv superEnv,TypeInfo returnType){
		super(superEnv);
		this.returnType = returnType;
	    addFunction("return",RETURN);
	    addFunction("if",IF);
	    addFunction("while",WHILE);
	    addFunction("break",BREAK);
	    addFunction("try",TRY_CATCH_FINALLY);
		addFunction("for",FOR);
	}
	@Override
	public TypeInfo getReturnType(){
		returnNotUsed = false;
		return returnType;
	}
	@Override
	public TypeInfo getReturnTypeInternal(){
		return returnType;
	}
	@Override
	public boolean isInMainBlock(){
		return false;
	}
	public final void compileFunctionBlock(StringBuilder builder,ExpIterator it)throws CompilerException{
		var exp = it.next();
		if(exp instanceof Atom a && a.getValue().equals("=")){
			exp = it.nextAtom();
		}
		compileBlock(exp,builder,it);
		if(exp instanceof Atom){
			builder.append(";\n");
		}
	}
}