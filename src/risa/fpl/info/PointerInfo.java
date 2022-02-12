package risa.fpl.info;

import risa.fpl.FPL;
import risa.fpl.env.AEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.exp.*;

public class PointerInfo extends TypeInfo{
	private final TypeInfo type;
    private boolean constant;
	public PointerInfo(TypeInfo type){
	    super(type.getName() + '*',type.getCname() + '*');
        this.type = type;
        if(type != TypeInfo.VOID){
            addField("+",new BinaryOperator(this,NumberInfo.MEMORY,"+"));
            addField("-",new BinaryOperator(this,NumberInfo.MEMORY,"-"));
            addField("*",new BinaryOperator(this,NumberInfo.MEMORY,"*"));
            addField("/",new BinaryOperator(this,NumberInfo.MEMORY,"/"));
            addField("%",new BinaryOperator(this,NumberInfo.MEMORY,"%"));
            addField("get",new GetElement(type));
            addField("set",new SetElement(type));
            addField("drf",type instanceof FunctionInfo f?new FunctionDereference(f.getFunction()):new Dereference(type));
        }
        addField("==",new BinaryOperator(BOOL,this,"=="));
        addField("!=",new BinaryOperator(BOOL,this,"!="));
        addField(">",new BinaryOperator(TypeInfo.BOOL,this,">"));
        addField("<",new BinaryOperator(TypeInfo.BOOL,this,"<"));
        addField(">=",new BinaryOperator(TypeInfo.BOOL,this,">="));
        addField("<=",new BinaryOperator(TypeInfo.BOOL,this,"<="));
        addField("cast",new Cast(this));
        addField("getObjectSize",PointerSize.INSTANCE);
        var cName = "free";
        if(type instanceof InstanceInfo i){
          cName = i.getInstanceFree();
        }
        if(type instanceof InstanceInfo i && i.getDestructorName() != null){
            var mod = i.getModule();
            var freeArray = FPL.getFreeArray();
            if(freeArray != null && !(i instanceof TemplateTypeInfo)){
                addField("free[]",freeArray.makeMethodFromTemplate(this,new TypeInfo[]{NumberInfo.MEMORY},mod));
            }
        }else{
            var f = new Function("free[]",TypeInfo.VOID,FunctionType.NATIVE,this,AccessModifier.PUBLIC);
            var freeArray = "_std_backend_freeLEFT_SQUARE_BRACKETRIGHT_SQUARE_BRACKET0";
            f.addVariant(new TypeInfo[]{NumberInfo.MEMORY},freeArray,freeArray);
            addField("free[]",f);
        }
        var f = new Function("free",TypeInfo.VOID,FunctionType.NATIVE,this,AccessModifier.PUBLIC);
        f.addVariant(new TypeInfo[0],cName,cName);
        addField("free",f);
        setClassInfo(ClassInfo.POINTER);
	}
	@Override
	public final boolean equals(Object o){
		return o instanceof PointerInfo p?type.equals(p.type):o == NIL;
    }
    @Override
    public final AField getField(String name,AEnv from){
	    var field = super.getField(name,from);
	    if(field == null){
	        field = type.getField(name,from);
        }
	    return field;
    }
    @Override
    public final String ensureCast(TypeInfo to,String expCode){
        return type.ensureCast(to,expCode,true);
    }
    @Override
    public final String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer){
	    return type.ensureCast(to,expCode,comesFromPointer);
    }
    @Override
    public final String ensureCast(TypeInfo to,String expCode,boolean comesFromPointer,boolean returnedByFunction){
	    return type.ensureCast(to,expCode,comesFromPointer,returnedByFunction);
    }
    public final TypeInfo getType(){
	    return type;
    }
    @Override
    public String getCname(){
	    return (constant?"const ":"") + super.getCname();
    }
    public final void makeConstant(){
        constant = true;
    }
}