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
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public final class ClassBlock extends ATwoPassBlock implements IFunction {

	@Override
	public TypeInfo compile(BufferedWriter writer, AEnv env, ExpIterator it, int line, int charNum) throws IOException, CompilerException {
		if(!(env instanceof ModuleEnv modEnv)){
		    throw new CompilerException(line,charNum,"can only be used on module level");
        }
        var id = it.nextID();
		var idV = id.getValue();
		if(env.hasTypeInCurrentEnv(idV)){
		    throw new CompilerException(id,"this type is already declared");
        }
        String cID;
        if(env.hasModifier(Modifier.NATIVE)){
            cID = id.getValue();
            if(!IFunction.isCId(cID)){
                throw new CompilerException(id,"invalid C identifier");
            }
        }else{
            cID = IFunction.toCId(id.getValue());
        }
        var cEnv = new ClassEnv(modEnv,cID,idV);
		TypeInfo parentType = null;
		List block = null;
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
                var typeID = (Atom)exp;
                if(typeID.getType() != TokenType.ID){
                    throw new CompilerException(typeID,"identifier expected");
                }
                var type = env.getType(typeID);
                if(type.isPrimitive()){
                    throw new CompilerException(typeID,"primitive types cannot inherited from");
                }
                cEnv.getInstanceType().addParent(type);
                if(type instanceof InterfaceInfo){

                }else{
                    if(parentType != null){
                        throw new CompilerException(typeID,"there is already parent class");
                    }
                    parentType = type;
                }
            }
        }
        if(block == null){
            throw new CompilerException(line,charNum,"block expected as last argument");
        }
		var b = new BuilderWriter(writer);
		b.write("typedef struct ");
	    b.write(IFunction.toCId(id.getValue()));
	    b.write("{\n");
	    if(parentType != null){
	        b.write(parentType.getCname());
	        b.write(" parent;\n");
        }
        try{
            compile(b,cEnv,block);
        }catch(CompilerException ex){
            ex.setSourceFile("");
            throw ex;
        }
	    b.write('}');
	    b.write(IFunction.toCId(id.getValue()));
	    b.write(";\n");
        var type = cEnv.getInstanceType();
        writer.write(b.getText());
        var constructor = type.getConstructor();
        if(constructor == null){
            constructor = new ClassVariable(type,cEnv.getClassType(),new TypeInfo[]{},cEnv.getNameSpace(this));
            cEnv.addMethod(constructor,cEnv.getDefaultConstructor());
            type.setConstructor(constructor);
        }
        writer.write(cEnv.getMethodCode());
        writer.write(cID);
        writer.write("* ");
        var newName = new StringBuilder();
        newName.append(modEnv.getNameSpace(this));
        newName.append(cID);
        newName.append("_new");
        type.appendToDeclaration(b.getText());
        cEnv.appendDeclarations();
        if(!env.hasModifier(Modifier.ABSTRACT)){
            writer.write(newName.toString());
            writer.write('(');
            var args = constructor.getArguments();
            var first = true;
            for(int i = 0; i < args.length;++i){
                if(first){
                    first = false;
                }else{
                    writer.write(',');
                }
                writer.write(args[i].getCname());
                writer.write(" a");
                writer.write(Integer.toString(i));
            }
            writer.write("){\n");
            writer.write("void* malloc(unsigned long);\n");
            writer.write(type.getCname());
            writer.write("* p=malloc(sizeof ");
            writer.write(type.getCname());
            writer.write(");\n");
            writer.write(constructor.getCname());
            writer.write("(p");
            for(int i = 0; i < constructor.getArguments().length;++i){
                writer.write(",a");
                writer.write(Integer.toString(i));
            }
            writer.write(");");
            writer.write("\nreturn p;\n}\n");
            var newMethod = Function.newNew(newName.toString(),type,constructor.getArguments());
            cEnv.getClassType().addField("new",newMethod);
            writer.write(newMethod.getDeclaration());
            type.appendToDeclaration(newMethod.getDeclaration());
        }
        type.buildDeclaration();
	    env.addType(idV,type);
	    env.addFunction(id.getValue(),constructor);
		return TypeInfo.VOID;
	}
}