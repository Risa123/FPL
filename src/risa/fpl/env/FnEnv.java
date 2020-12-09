package risa.fpl.env;

import risa.fpl.function.block.ConditionalBlock;
import risa.fpl.function.block.TryCatchFinally;
import risa.fpl.function.statement.Break;
import risa.fpl.function.statement.Return;
import risa.fpl.info.TypeInfo;

public final class FnEnv extends FnSubEnv{
	private static final Return RETURN = new Return();
	private static final ConditionalBlock IF = new ConditionalBlock("if");
	private static final ConditionalBlock WHILE = new ConditionalBlock("while");
	private static final Break BREAK = new Break();
	private static final TryCatchFinally TRY_CATCH_FINALLY = new TryCatchFinally();
	private final TypeInfo returnType;
	private boolean returnUsed;
	private int catchNum;
	public FnEnv(AEnv superEnv,TypeInfo returnType) {
		super(superEnv);
		this.returnType  = returnType;
	    addFunction("return",RETURN);
	    addFunction("if",IF);
	    addFunction("while",WHILE);
	    addFunction("break",BREAK);
	    addFunction("try",TRY_CATCH_FINALLY);
	}
	@Override
	public TypeInfo getReturnType() {
		returnUsed = true;
		return returnType;
	}
	public boolean notReturnUsed() {
		return !returnUsed;
	}
	@Override
    public int getCatchNum(){
	    return catchNum++;
    }
}