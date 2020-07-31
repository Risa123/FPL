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
import risa.fpl.function.exp.Function;
import risa.fpl.function.statement.ClassVariable;
import risa.fpl.info.ClassInfo;
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
		var cEnv = new ClassEnv(modEnv,cID);
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
	    writer.write(cID);
	    writer.write("* ");
	    var newName = new StringBuilder();
        newName.append(modEnv.getNameSpace());
        newName.append(cID);
        newName.append("_new");
        b.write(cID);
        b.write("* ");
        b.write(newName.toString());
        b.write("();\n");
        var type = new TypeInfo(id.value,cID,b.getText(),null);
        writer.write(newName.toString());
        writer.write('(');
        writer.write(type.cname);
        writer.write("* this){\n");
        writer.write("void* malloc(unsigned long);\n");
        writer.write(type.cname);
        writer.write("* p=malloc(sizeof ");
        writer.write(type.cname);
        writer.write(");\n");
        writer.write(modEnv.getNameSpace());
        writer.write(type.cname);
        writer.write("__init(p);\nreturn p;\n}");
	    var classType = new ClassInfo(id.value);
	    classType.addField("new",Function.newNew(newName.toString(),type));
	    type.setClassInfo(classType);
	    cEnv.addFields(type);
	    env.addType(id.value,type);
	    env.addFunction(id.value,new ClassVariable(type,classType,new TypeInfo[]{}));
		return TypeInfo.VOID;
	}
}