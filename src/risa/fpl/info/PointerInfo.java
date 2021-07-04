package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.exp.*;

public final class PointerInfo extends TypeInfo implements IPointerInfo{
	private final TypeInfo type;
	public PointerInfo(TypeInfo type){
	    super(type.getName() + "*",type.getCname() + "*",true);
        this.type = type;
        if(type != TypeInfo.VOID){
            addField("+", new BinaryOperator(this,NumberInfo.MEMORY, "+"));
            addField("-", new BinaryOperator(this,NumberInfo.MEMORY, "-"));
            addField("*", new BinaryOperator(this,NumberInfo.MEMORY, "*"));
            addField("/", new BinaryOperator(this,NumberInfo.MEMORY, "/"));
            addField("%", new BinaryOperator(this,NumberInfo.MEMORY, "%"));
            addField("get",new GetElement(type));
            addField("set",new SetElement(type));
            if(type instanceof FunctionInfo f){
               addField("drf",new FunctionDereference(f.getFunction()));
            }else{
              addField("drf",new Dereference(type));
            }
        }
        addField("==", new BinaryOperator(BOOL,this, "=="));
        addField("!=", new BinaryOperator(BOOL,this, "!="));
        addField(">",new BinaryOperator(TypeInfo.BOOL,this,">"));
        addField("<",new BinaryOperator(TypeInfo.BOOL,this,"<"));
        addField(">=",new BinaryOperator(TypeInfo.BOOL,this,">="));
        addField("<=",new BinaryOperator(TypeInfo.BOOL,this,"<="));
        addField("cast", new Cast(this));
        addField("getObjectSize",PointerSize.INSTANCE);
        var cName = "free";
        if(type instanceof InstanceInfo i){
          cName = i.getInstanceFree();
        }
        var f = new Function("free",TypeInfo.VOID,FunctionType.NATIVE,type,AccessModifier.PUBLIC);
        f.addVariant(new TypeInfo[0],cName,cName);
        addField("free",f);
        setClassInfo(ClassInfo.POINTER);
	}
	@Override
	public boolean equals(Object o){
		if(o instanceof PointerInfo p){
			return type.equals(p.type);
		}else return o == NIL;
    }
    @Override
    public IField getField(String name,AEnv from){
	    var field = super.getField(name,from);
	    if(field == null){
	        field = type.getField(name,from);
        }
	    return field;
    }
    @Override
    public String getConversionMethodCName(TypeInfo type){
        return this.type.getConversionMethodCName(type);
    }
    @Override
    public String ensureCast(TypeInfo to,String expCode){
        return type.ensureCast(to,expCode,true);
    }
    public TypeInfo getType(){
	    return type;
    }
    @Override
    public String getPointerVariableDeclaration(String cID){
	    if(type instanceof IPointerInfo p){
	        return p.getPointerVariableDeclaration("*" + cID);
        }
	    return getCname() + " " + cID;
    }
}