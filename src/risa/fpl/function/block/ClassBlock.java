package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.ClassEnv;
import risa.fpl.env.Modifier;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;

public final class ClassBlock implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		var id = it.nextID();
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,charNum,"can only be used on module level");
        }
        String cID;
        if(env.hasModifier(Modifier.NATIVE)){
            cID = id.value;
            if(!IFunction.isCId(cID)){
                throw new CompilerException(id,"invalid C identifier");
            }
        }else{
            cID = IFunction.toCId(id.value);
        }
		var cEnv = new ClassEnv(env,cID);
		var b = new BuilderWriter(writer);
		b.write("typedef struct ");
	    b.write(IFunction.toCId(id.value));
	    b.write("{\n");
		it.nextList().compile(b,cEnv, it);
	    b.write('}');
	    b.write(IFunction.toCId(id.value));
	    b.write(";\n");
	    writer.write(b.getText());
        writer.write(cEnv.getDefaultConstructor());
	    var type = new TypeInfo(id.value,cID,b.getText(),null);
	    var newName = new StringBuilder(type.cname);
        newName.append(' ');
        newName.append(modEnv.getNameSpace());
        newName.append(cID);
        writer.write(newName.toString());
        writer.write('(');
        writer.write(type.cname);
        writer.write("* this){");
        writer.write(type.cname);
        writer.write(" p=malloc(sizeof ");
        writer.write(type.cname);
        writer.write(");");
        writer.write(modEnv.getNameSpace());
        writer.write(type.cname);
        writer.write("_init(p);return p;}");
	    var classType = new TypeInfo(id.value,"");
	    cEnv.addFields(type);
	    env.addType(id.value,type);
	    env.addFunction(id.value,new ClassVariable(type,classType,new TypeInfo[]{}));
		return TypeInfo.VOID;
	}
}