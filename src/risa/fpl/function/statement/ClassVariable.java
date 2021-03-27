package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TemplateTypeInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class ClassVariable extends Function{
   private final TypeInfo type,classType;
   public ClassVariable(TypeInfo type,TypeInfo classType,TypeInfo[]args,String nameSpace){
       super("constructor",TypeInfo.VOID,makeCName(nameSpace),args,FunctionType.NORMAL,type,AccessModifier.PUBLIC,makeCName(nameSpace));
	   this.type = type;
	   this.classType = classType;
       if(type instanceof TemplateTypeInfo){
           setDeclaration("");
       }
   }
   private static String makeCName(String nameSpace){
      return INTERNAL_METHOD_PREFIX + nameSpace + "_init";
   }
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        BuilderWriter b = new BuilderWriter(writer);
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
        TypeInfo varType;
        if(type instanceof TemplateTypeInfo tType){
            varType = tType.generateTypeFor(IFunction.parseTemplateGeneration(it,env,true),env,it.getLastLine(),it.getLastCharNum());
            if(it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
                return varType;
            }
            id = it.nextID(); //identifier follows after template arguments
        }else{
          varType = type;
        }
        writer.write(varType.getCname());
        writer.write(' ');
        var cID = IFunction.toCId(id.getValue());
        writer.write(cID);
        writer.write(";\n");
        setPrevCode(cID);
        var b = new BuilderWriter(writer);
        if(type instanceof TemplateTypeInfo){
            varType.getConstructor().setPrevCode(cID);
            ((ClassVariable)varType.getConstructor()).superCompile(b,env,it,id.getLine(),id.getCharNum());
        }else{
            super.compile(b,env,it,id.getLine(),id.getCharNum());
        }
        if(env.hasFunctionInCurrentEnv(id.getValue())){
            throw new CompilerException(id,"there is already a function called " + id);
        }
        env.addFunction(id.getValue(),new Variable(varType,IFunction.toCId(id.getValue()),id.getValue()));
        if(env instanceof ClassEnv e){
            e.appendToImplicitConstructor(b.getCode() + ";\n");
        }else if(env instanceof ModuleEnv e){
            e.appendToInitializer(b.getCode() + ";\n");
        }else{
            writer.write(b.getCode());
        }
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
            return field.compile(writer,env,it,atom.getLine(),atom.getCharNum());
        }
        return classType;
    }
}