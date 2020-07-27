package risa.fpl.info;

import risa.fpl.function.exp.BinaryOperator;
import risa.fpl.function.exp.GetIndex;
import risa.fpl.function.exp.SetIndex;
import risa.fpl.function.exp.UnaryOperator;

public final class PointerInfo extends TypeInfo {
	private final TypeInfo type;
	public PointerInfo(TypeInfo type) {
	    this(type,type.name +"*",type.cname + "*");
	}
	//function pointer constructor
	public PointerInfo(String cname,String name,TypeInfo returnType,TypeInfo[]args){
	    this(null,name + "*",returnType.cname + "(*" + cname + ")(" + argsToString(args)  +")");
    }
    private PointerInfo(TypeInfo type,String name,String cname){
	    super(name,cname);
        this.type = type;
        addField("++",new UnaryOperator(this,"++",false));
        addField("--",new UnaryOperator(this,"--",false));
        addField("+",new BinaryOperator(this,this,"+"));
        addField("-",new BinaryOperator(this,this,"-"));
        addField("*",new BinaryOperator(this,this,"*"));
        addField("/",new BinaryOperator(this,this,"/"));
        addField("%",new BinaryOperator(this,this,"%"));
        addField("get",new GetIndex(this));
        addField("set",new SetIndex());
        addField("==",new BinaryOperator(BOOL,this,"=="));
        addField("!=",new BinaryOperator(BOOL,this,"!="));
    }
	@Override
	public boolean equals(Object o) {
		if(o instanceof PointerInfo p) {
			return type.equals(p.type);
		}else return o == TypeInfo.NULL;
    }
    private static String argsToString(TypeInfo[]args){
	    var b = new StringBuilder();
	    var first = true;
	    for(var arg:args){
	        if(first){
	            first = false;
            }else{
	            b.append(',');
            }
	        b.append(arg.cname);
        }
	    return b.toString();
    }
}