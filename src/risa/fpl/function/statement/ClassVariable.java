package risa.fpl.function.statement;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.tokenizer.TokenType;

public final class ClassVariable extends Function {
   private final TypeInfo type,classType;
   public ClassVariable(TypeInfo type,TypeInfo classType,TypeInfo[]args) {
       super("constructor",TypeInfo.VOID,type.cname + "__init",args,false,type);
	   this.type = type;
	   this.classType = classType;
   }
	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
        var id = it.nextAtom();
		if(id.type == TokenType.ID){
            writer.write(type.cname);
            writer.write(' ');
            var cID = IFunction.toCId(id.value);
            writer.write(cID);
            writer.write(";\n");
            setPrevCode(cID);
            super.compile(writer,env,it,line,charNum);
            env.addFunction(id.value,new Variable(type,IFunction.toCId(id.value),id.value));
        }else if(id.type == TokenType.CLASS_SELECTOR){
		    return classType;
        }else{
		    throw new CompilerException(id,"variable identifier or : expected");
        }
		return TypeInfo.VOID;
	}
}