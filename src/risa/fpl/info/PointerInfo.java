package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.*;

public final class PointerInfo extends TypeInfo{
	private final TypeInfo type;
	private final boolean array;
	public PointerInfo(TypeInfo type,boolean array){
	    super(type.getName() +"*",type.getCname() + "*",true);
        this.type = type;
        this.array = array;
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
        var cName = "free";
        if(type instanceof InstanceInfo i){
          cName = i.getInstanceFree();
        }
        var f = new Function("free",TypeInfo.VOID,FunctionType.NORMAL,type,AccessModifier.PUBLIC);
        f.addVariant(new TypeInfo[0],cName,cName);
        addField("free",f);
	}
	public PointerInfo(TypeInfo type){
	    this(type,false);
    }
	@Override
	public boolean equals(Object o){
		if(o instanceof PointerInfo p){
			return type.equals(p.type);
		}else return o == TypeInfo.NIL;
    }
    public boolean isFunctionPointer(){
	    return type instanceof FunctionInfo;
    }
    public String getFunctionPointerDeclaration(String cID){
	    var f = ((FunctionInfo)type).getFunction();
        var b = new StringBuilder(f.getReturnType().getCname());
        b.append("(*").append(cID).append(")(");
        var self = f.getSelf();
        var firstArg = self == null;
        if(!firstArg){
            if(self instanceof InterfaceInfo){
               b.append("void");
            }else{
                b.append("struct ").append(self.getCname());
            }
            b.append("* this");
        }
        var variant = f.getPointerVariant();
        for(var arg:variant.args()){
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
    public String getCname(){
	    if(type instanceof FunctionInfo){
	        return getFunctionPointerDeclaration(IFunction.toCId(getName()));
        }
        return super.getCname();
    }
    public boolean isArray(){
	    return array;
    }
}