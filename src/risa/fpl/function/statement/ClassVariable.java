package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
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
        var id = it.nextAtom();
		if(id.getType() == TokenType.ID){
            compileVariable(writer,id,env,it,null);
        }else if(id.getType() == TokenType.CLASS_SELECTOR){
		    if(it.peek() instanceof Atom atom && atom.getType() == TokenType.ID){
		        it.next();
		        var field = classType.getField(atom.getValue(),env);
		        if(field == null){
		            throw new CompilerException(atom,classType + " has no field called " + atom);
		        }
		        return field.compile(writer,env,it,line,atom.getCharNum());
            }
		    return classType;
        }else if(id.getType() == TokenType.END_ARGS){
            compileVariable(writer,null,env,it,IFunction.getTemplateArguments(it));
        }else{
		    throw new CompilerException(id,"variable identifier or : expected");
        }
		return TypeInfo.VOID;
	}
	private void compileVariable(BufferedWriter writer,Atom id,AEnv env,ExpIterator it,ArrayList<Atom> args)throws IOException,CompilerException{
        TypeInfo varType;
        if(type instanceof TemplateTypeInfo tType){
            var types = new ArrayList<TypeInfo>();
            for(var arg:args){
                types.add(env.getType(arg));
            }
            varType = tType.generateTypeFor(types,writer);
            id = it.nextID(); // identifier follows after template arguments
        }else{
          varType = type;
        }
        writer.write(varType.getCname());
        writer.write(' ');
        var cID = IFunction.toCId(id.getValue());
        writer.write(cID);
        writer.write(";\n");
        setPrevCode(cID);
        if(type instanceof TemplateTypeInfo){
            varType.getConstructor().setPrevCode(cID);
            ((ClassVariable)varType.getConstructor()).superCompile(writer,env,it,id.getLine(),id.getCharNum());
        }else{
            super.compile(writer,env,it,id.getLine(),id.getCharNum());
        }
        env.addFunction(id.getValue(),new Variable(type,IFunction.toCId(id.getValue()),id.getValue()));
    }
	public void compileAsParentConstructor(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
       calledOnPointer();
       super.compile(writer,env,it,line,charNum);
    }
    private void superCompile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
       super.compile(writer,env,it,line,charNum);
    }
}