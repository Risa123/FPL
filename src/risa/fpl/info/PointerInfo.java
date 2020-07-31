package risa.fpl.info;

import risa.fpl.function.exp.*;

public final class PointerInfo extends TypeInfo {
	private final TypeInfo type;
	public PointerInfo(TypeInfo type) {
	    super(type.name +"*",type.cname + "*");
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
        var b = new StringBuilder(f.returnType.cname);
        b.append("(*");
        b.append(cID);
        b.append(")(");
        var firstArg = true;
        for(var arg:f.args){
            if(firstArg){
                firstArg = false;
            }else{
                b.append(',');
            }
            b.append(arg);
        }
        b.append(")");
        return b.toString();
    }
}