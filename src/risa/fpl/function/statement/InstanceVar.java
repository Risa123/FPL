package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.ClassInfo;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class InstanceVar extends Function{
   private final ClassInfo classType;
   private final InstanceInfo type;
   public InstanceVar(InstanceInfo type, ClassInfo classType){
       super("constructor",TypeInfo.VOID,FunctionType.NORMAL,type,AccessModifier.PUBLIC);
	   this.type = type;
	   this.classType = classType;
   }
   private String makeCName(String nameSpace){
      return INTERNAL_METHOD_PREFIX + nameSpace + "_init";
   }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        BuilderWriter b = new BuilderWriter();
        var id = it.nextAtom();
		if(id.getType() == TokenType.ID){
            compileVariable(b,id,env,it);
        }else if(id.getType() == TokenType.CLASS_SELECTOR){
		    return compileClassSelector(it,env,writer,classType);
        }else if(id.getType() == TokenType.END_ARGS){
            var varType = compileVariable(b,null,env,it);
            if(it.hasNext() && it.peek() instanceof Atom atom && atom.getType() == TokenType.CLASS_SELECTOR){
                it.next();
                return compileClassSelector(it,env,writer,varType.getClassInfo());
            }
        }else{
		    throw new CompilerException(id,"variable identifier or : expected");
        }
        writer.write(b.getCode());
		return TypeInfo.VOID;
	}
	private TypeInfo compileVariable(BufferedWriter writer,Atom id,AEnv env,ExpIterator it)throws IOException,CompilerException{
        InstanceInfo varType;
        if(type instanceof TemplateTypeInfo tType){
            varType = tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env,true),env,it.getLastLine(),it.getLastCharNum());
            if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
                return varType;
            }
            id = it.nextID();//identifier follows after template arguments
        }else{
          varType = type;
        }
        var typeCname = varType.getCname();
        var notPointer = true;
        if(id.getValue().equals("*")){
            id = it.nextID();
            notPointer = false;
            typeCname += "*";
        }
        var cID = IFunction.toCId(id.getValue());
        writer.write(typeCname + ' ' + cID + ";\n");
        var b = new BuilderWriter();
        if(notPointer){
            if(env instanceof ClassEnv){
                setPrevCode("this->" + cID);
            }else{
                setPrevCode(cID);
            }
            if(type instanceof TemplateTypeInfo){
                varType.getConstructor().setPrevCode(getPrevCode());
                varType.getConstructor().superCompile(b,env,it,id.getLine(),id.getTokenNum());
            }else{
                super.compile(b,env,it,id.getLine(),id.getTokenNum());
            }
        }
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"there is already a function called " + id);
        }
        var vID = id.getValue();
        var varCid = IFunction.toCId(vID);
        TypeInfo instanceType = null;
        if(env instanceof ClassEnv e){
            instanceType = e.getInstanceType();
        }
        if(env instanceof ClassEnv e){
            e.appendToImplicitConstructor(b.getCode() + ";\n");
        }else if(env instanceof ModuleEnv e){
            e.appendToInitializer(b.getCode() + ";\n");
        }else{
            writer.write(b.getCode());
        }
        env.addFunction(id.getValue(),new Variable(varType,varCid,false,vID,env.hasModifier(Modifier.CONST),instanceType,env.getAccessModifier()));
        ((SubEnv)env).addInstanceVariable(varType,varCid);
        return null;
    }
	public void compileAsParentConstructor(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
       calledOnPointer();
       super.compile(writer,env,it,line,charNum);
    }
    private void superCompile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
       super.compile(writer,env,it,line,charNum);
    }
    private TypeInfo compileClassSelector(ExpIterator it,AEnv env,BufferedWriter writer,TypeInfo classType)throws CompilerException,IOException{
        if(it.peek() instanceof Atom atom && atom.getType() == TokenType.ID){
            it.next();
            var field = classType.getField(atom.getValue(),env);
            if(field == null){
                throw new CompilerException(atom,classType + " has no field called " + atom);
            }
            return field.compile(writer,env,it,atom.getLine(),atom.getTokenNum());
        }
        return classType;
    }
    public void addVariant(TypeInfo[]args,String nameSpace){
       addVariant(args,makeCName(nameSpace),makeCName(nameSpace));
    }
}