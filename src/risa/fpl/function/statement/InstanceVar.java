package risa.fpl.function.statement;

import java.io.IOException;
import java.io.UncheckedIOException;

import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.FunctionVariant;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.AtomType;

public final class InstanceVar extends Function{
   private final InstanceInfo type;
   public InstanceVar(InstanceInfo type){
       super("constructor",TypeInfo.VOID,FunctionType.NORMAL,type,AccessModifier.PUBLIC);
	   this.type = type;
   }
   private String makeCName(String nameSpace){
      return INTERNAL_METHOD_PREFIX + nameSpace + "_init";
   }
	@Override
	public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum,Modifier.CONST);
        var b = new StringBuilder();
        var id = it.nextAtom();
		if(id.getType() == AtomType.ID){
            if(env instanceof ClassEnv e){
                e.getVariableFieldDeclarationOrder().add(id.getValue());
            }
            compileVariable(b,id,env,it);
        }else if(id.getType() == AtomType.CLASS_SELECTOR){
		    return compileClassSelector(it,env,builder,type.getClassInfo());
        }else if(id.getType() == AtomType.END_ARGS){
            var varType = compileVariable(b,null,env,it);
            if(it.hasNext() && it.peek() instanceof Atom atom && atom.getType() == AtomType.CLASS_SELECTOR){
                it.next();
                //noinspection ConstantConditions
                return compileClassSelector(it,env,builder,varType.getClassInfo());
            }
        }else{
		    throw new CompilerException(id,"variable identifier or : expected");
        }
        if(env instanceof ModuleEnv e){
            e.appendVariableDeclaration(b.toString());
        }else{
            builder.append(b);
        }
		return TypeInfo.VOID;
	}
	private TypeInfo compileVariable(StringBuilder builder,Atom id,SubEnv env,ExpIterator it)throws CompilerException{
        InstanceInfo varType;
        if(type instanceof TemplateTypeInfo tType){
            try{
                varType = tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env,true),env,it.getLastLine(),it.getLastCharNum());
            }catch(IOException e){
                throw new UncheckedIOException(e);
            }
            if(it.peek() instanceof Atom a && a.getType() == AtomType.CLASS_SELECTOR){
                return varType;
            }
            id = it.nextID();//identifier follows after template arguments
            if(env instanceof ClassEnv e){
                e.getVariableFieldDeclarationOrder().add(id.getValue());
            }
        }else{
          varType = type;
        }
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            error(id,"there is already a function called " + id);
        }
        var typeCname = varType.getCname();
        var notPointer = true;
        if(id.getValue().equals("*")){
            id = it.nextID();
            notPointer = false;
            typeCname += '*';
        }
        var cID = IFunction.toCId(id.getValue());
        builder.append(typeCname).append(' ').append(cID).append(";\n");
        if(it.hasNext()){
            if(it.peek() instanceof Atom a && a.getValue().equals("init")){
                it.next();
                var b = new StringBuilder();
                if(notPointer){
                    setPrevCode(env instanceof ClassEnv?"this->" + cID:cID);
                    if(type instanceof TemplateTypeInfo){
                        varType.getConstructor().setPrevCode(getPrevCode());
                        varType.getConstructor().superCompile(b,env,it,id.getLine(),id.getTokenNum());
                    }else{
                        super.compile(b,env,it,id.getLine(),id.getTokenNum());
                    }
                }
                env.addInstanceVariable(varType,cID);
                if(env instanceof ClassEnv e){
                    e.appendToImplicitConstructor(b + ";\n");
                }else if(env instanceof ModuleEnv e){
                    e.appendToInitializer(b + ";\n");
                }else{
                    builder.append(b).append(";\n");
                }
            }else{
                error(id,"init(constructor arguments) or nothing expected");
            }
        }
        var instanceType = env instanceof ClassEnv e?e.getInstanceInfo():null;
        env.addFunction(id.getValue(),new Variable(varType,cID,false,id.getValue(),env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier()));
        return null;
    }
	public void compileCallInsideOfConstructor(StringBuilder builder,FnSubEnv env,ExpIterator it,int line,int charNum)throws CompilerException{
       calledOnPointer();
       var tmp = new StringBuilder();
       super.compile(tmp,env,it,line,charNum);
       builder.append(env.getToPointerVars()).append(tmp);
       if(!env.getDestructorCalls().isEmpty()){
           builder.append(";\n").append(env.getDestructorCalls());
       }
    }
    private void superCompile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int charNum)throws CompilerException{
       super.compile(builder,env,it,line,charNum);
    }
    private TypeInfo compileClassSelector(ExpIterator it,SubEnv env,StringBuilder builder,TypeInfo classType)throws CompilerException{
        if(it.peek() instanceof Atom atom && atom.getType() == AtomType.ID){
            it.next();
            var field = classType.getField(atom.getValue(),env);
            if(field == null){
                throw new CompilerException(atom,classType + " has no field called " + atom);
            }
            var ret = field.compile(builder,env,it,atom.getLine(),atom.getTokenNum());
            builder.append(";\n");
            return ret;
        }
        return classType;
    }
    public FunctionVariant addVariant(TypeInfo[]args,String nameSpace){
       return addVariant(args,makeCName(nameSpace),makeCName(nameSpace));
    }
    @Override
    public boolean appendSemicolon(){
        return false;
    }
}