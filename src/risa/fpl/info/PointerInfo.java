package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.exp.*;

public final class PointerInfo extends TypeInfo {
	private final TypeInfo type;
	public PointerInfo(TypeInfo type) {
	    super(type.getName() +"*",type.getCname() + "*",true);
        this.type = type;
        if(type != TypeInfo.VOID){
            addField("+",new BinaryOperator(this,this,"+"));
            addField("-",new BinaryOperator(this,this,"-"));
            addField("*",new BinaryOperator(this,this,"*"));
            addField("/",new BinaryOperator(this,this,"/"));
            addField("%",new BinaryOperator(this,this,"%"));
            addField("get",new GetIndex(this));
            addField("set",new SetIndex());
            addField("==",new BinaryOperator(BOOL,this,"=="));
            addField("!=",new BinaryOperator(BOOL,this,"!="));
            addField("cast",new Cast(this));
            if(type instanceof Function){
                addField("drf",(Function)type);
            }else{
                addField("drf",new UnaryOperator(type,"*",false));
            }
        }
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof PointerInfo p) {
			return type.equals(p.type);
		}else return o == TypeInfo.NIL;
    }
    public boolean isFunctionPointer(){
	    return type instanceof Function;
    }
    public String getFunctionPointerDeclaration(String cID){
	    var f = (Function)type;
        var b = new StringBuilder(f.getReturnType().getCname());
        b.append("(*");
        b.append(cID);
        b.append(")(");
        var self = f.getSelf();
        var firstArg = self == null;
        if(!firstArg){
            if(self instanceof InterfaceInfo){
               b.append("void");
            }else{
                b.append("struct ");
                b.append(self.getCname());
            }
            b.append("* this");
        }
        for(var arg:f.getArguments()){
            if(firstArg){
                firstArg = false;
            }else{
                b.append(',');
            }
            b.append(arg.getCname());
        }
        b.append(")");
        return b.toString();
    }

    @Override
    public IField getField(String name, AEnv from) {
	    var field = super.getField(name,from);
	    if(field == null){
	        field = type.getField(name,from);
        }
	    return field;
    }
    @Override
    public String getConversionMethodCName(TypeInfo type) {
        return this.type.getConversionMethodCName(type);
    }

    @Override
    public String ensureCast(TypeInfo to, String expCode) {
        return type.ensureCast(to,expCode,true);
    }
    public TypeInfo getType(){
	    return type;
    }
}