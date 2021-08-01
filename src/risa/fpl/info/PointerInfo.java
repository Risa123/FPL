package risa.fpl.info;

import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.exp.*;

public final class PointerInfo extends TypeInfo implements IPointerInfo{
	private final TypeInfo type;
	private int functionPointerDepth;
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
        if(type instanceof InstanceInfo i && i.getDestructorName() != null){
            var mod = i.getModule();
            var freeArray = mod.getFPL().getFreeArray();
            if(freeArray != null){
                addField("free[]",freeArray.makeMethodFromTemplate(this,new TypeInfo[]{NumberInfo.MEMORY},mod));
            }
        }else{
            var f = new Function("free[]",TypeInfo.VOID,FunctionType.NATIVE,this,AccessModifier.PUBLIC);
            var freeArray = "_std_lang_freeLEFT_SQUARE_BRACKETRIGHT_SQUARE_BRACKET0";
            f.addVariant(new TypeInfo[]{NumberInfo.MEMORY},freeArray,freeArray);
            addField("free[]",f);
        }
        var f = new Function("free",TypeInfo.VOID,FunctionType.NATIVE,this,AccessModifier.PUBLIC);
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
    public String ensureCast(TypeInfo to,String expCode){
        return type.ensureCast(to,expCode,true);
    }
    @Override
    public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
	    return type.ensureCast(to,expCode,comesFromPointer);
    }
    @Override
    public String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer,boolean returnedByFunction){
	    return type.ensureCast(to,expCode,comesFromPointer,returnedByFunction);
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
    @Override
    public FunctionInfo getFunctionPointer(){
	    var t = type;
	    for(;;){
	        if(t instanceof FunctionInfo info){
	            return info;
            }else if(t instanceof PointerInfo p){
	            t = p.getType();
	            functionPointerDepth++;
            }else{
	            break;
            }
        }
        return null;
    }
    @Override
    public int getFunctionPointerDepth(){
	    return functionPointerDepth;
    }
    @Override
    public String getCname(){
	    var times = 1;
	    var t = type;
	    for(;;){
	        if(t instanceof FunctionInfo f){
	            return f.getPointerVariableDeclaration("*".repeat(times));
            }else if(t instanceof PointerInfo p){
	            t = p.getType();
	            times++;
            }else{
	            break;
            }
        }
	    return super.getCname();
    }
}