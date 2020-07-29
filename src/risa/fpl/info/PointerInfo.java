package risa.fpl.info;

import risa.fpl.function.exp.BinaryOperator;
import risa.fpl.function.exp.GetIndex;
import risa.fpl.function.exp.SetIndex;
import risa.fpl.function.exp.UnaryOperator;

public final class PointerInfo extends TypeInfo {
	private final TypeInfo type;
	public PointerInfo(TypeInfo type) {
	    super(type.name +"*",type.cname + "*");
        this.type = type;
        if(type != TypeInfo.VOID){
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
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof PointerInfo p) {
			return type.equals(p.type);
		}else return o == TypeInfo.NIL;
    }
}