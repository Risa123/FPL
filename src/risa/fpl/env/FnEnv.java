package risa.fpl.env;

import risa.fpl.function.block.ConditionalBlock;
import risa.fpl.function.statement.Break;
import risa.fpl.function.statement.Return;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.TypeInfo;

public final class FnEnv extends SubEnv implements IClassOwnedEnv{
	private static final Return RETURN = new Return();
	private static final ConditionalBlock IF = new ConditionalBlock("if");
	private static final ConditionalBlock WHILE = new ConditionalBlock("while");
	private static final Break BREAK = new Break();
	private final TypeInfo returnType;
	private boolean returnUsed;
	private final ClassInfo classType;
	public FnEnv(AEnv superEnv,TypeInfo returnType,ClassInfo classType) {
		super(superEnv);
		this.classType = classType;
		this.returnType  = returnType;
	    addFunction("return",RETURN);
	    addFunction("if",IF);
	    addFunction("while",WHILE);
	    addFunction("break",BREAK);
	}
	@Override
	public TypeInfo getReturnType() {
		returnUsed = true;
		return returnType;
	}
	public boolean isReturnUsed() {
		return returnUsed;
	}
    @Override
    public ClassInfo getClassType() {
        return classType;
    }
}