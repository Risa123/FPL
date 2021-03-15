package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.info.*;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

public class Fn extends AFunctionBlock{
	private boolean appendSemicolon;
	@Override
	public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
		var returnType = env.getType(it.nextID());
        var b = new BuilderWriter(writer);
		var id = it.nextID();
        if(env instanceof  ModuleEnv e && e.isMain() && id.getValue().equals("main")){
           throw new CompilerException(id,"main function can only be declared using build-in function main");
        }
	    String cID;
	    if(env.hasModifier(Modifier.NATIVE)){
	    	cID = id.getValue();
	    	if(IFunction.notCID(id.getValue())){
	    		throw new CompilerException(id,"invalid C identifier");
	    	}
	    }else{
	    	cID = "";
	    	if(!env.hasModifier(Modifier.ABSTRACT) && env instanceof ANameSpacedEnv tmp){
	    	    cID = tmp.getNameSpace(this);
	    	}
	    	cID += IFunction.toCId(id.getValue());
	    }
        TypeInfo self = null;
        if(env instanceof  ClassEnv cEnv){
            self = cEnv.getInstanceType();
        }else if(env instanceof InterfaceEnv e){
            self = e.getType();
        }
        var fnEnv = new FnEnv(env,returnType);
        var headWriter = new BuilderWriter(writer);
        if(env.getAccessModifier() == AccessModifier.PRIVATE){
            headWriter.write("static ");
        }
        headWriter.write(returnType.getCname());
        headWriter.write(' ');
        headWriter.write(cID);
		var args = parseArguments(headWriter,it,fnEnv,self);
		var attrCode = new StringBuilder();
        if(it.hasNext() && it.peek() instanceof Atom a && a.getType() == TokenType.CLASS_SELECTOR){
            it.next();
            var attrs = new ArrayList<String>();
            if(it.hasNext()){
                attrCode.append("__attribute__((");
            }
            while(it.hasNext()){
               if(it.peek() instanceof List){
                   break;
               }else{
                   var attr = it.nextID();
                   if(attrs.contains(attr.getValue())){
                       throw new CompilerException(attr,"attribute duplicity");
                   }
                   attrs.add(attr.getValue());
                   switch(attr.getValue()){
                       case "noReturn" ->{
                           if (attrs.contains("returnsTwice")) {
                               throw new CompilerException(attr,"noReturn is mutually exclusive with returnsTwice");
                           }
                           attrCode.append("__noreturn__");
                       }
                       case "returnsTwice" ->{
                           if (attrs.contains("noReturn")) {
                               throw new CompilerException(attr, "returnsTwice is mutually exclusive with noReturn");
                           }
                           attrCode.append("__returns_twice__");
                       }
                       default -> throw new CompilerException(attr,"no attribute is called " + attr);
                   }
               }
            }
            if(attrCode.length() > 0){
                attrCode.append("))");
                headWriter.write(attrCode.toString());
            }
        }
        var oneLine = false;
        var macroDeclaration = new StringBuilder();
		if(it.hasNext()){
		    if(env.hasModifier(Modifier.ABSTRACT)){
		        throw new CompilerException(line,charNum,"abstract methods can only be declared");
            }
            var block = it.next();
            if(block instanceof Atom a){
                oneLine = true;
                if(!(env.hasModifier(Modifier.VIRTUAL) || env.hasModifier(Modifier.OVERRIDE))){
                    macroDeclaration.append("#define ");
                    macroDeclaration.append(cID);
                    macroDeclaration.append('(');
                    if(self != null){
                        macroDeclaration.append("this");
                    }
                    var first = true;
                    for(var arg:args.keySet()){
                        if(first){
                            if(self != null){
                                macroDeclaration.append(',');
                            }
                            first = false;
                        }else{
                            macroDeclaration.append(',');
                        }
                        macroDeclaration.append(IFunction.toCId(arg));
                    }
                    macroDeclaration.append(')');
                }
                if(!a.getValue().equals("=")){
                    throw new CompilerException(a,"= expected");
                }
                block = it.next();
                fnEnv.getReturnType();
            }
            b.write(macroDeclaration.toString());
			if(macroDeclaration.isEmpty()){
                b.write(headWriter.getCode());
                b.write("{\n");
            }
			if(macroDeclaration.isEmpty() && oneLine && returnType != TypeInfo.VOID){
			    b.write("return ");
            }
			var code = new BuilderWriter(writer);
			block.compile(code,fnEnv,it);
			if(!macroDeclaration.isEmpty() && oneLine){
			    macroDeclaration.append('(');
			    var c = code.getCode();
			    if(c.endsWith(";\n")){
			      c =  c.substring(0,c.length() - 2);
                }
			    macroDeclaration.append(c);
			    macroDeclaration.append(")\n");
            }
			b.write(code.getCode());
			if(oneLine && macroDeclaration.isEmpty()){
			    b.write(";\n");
            }else if(oneLine){
			    b.write('\n');
            }
			if(fnEnv.notReturnUsed() && returnType != TypeInfo.VOID){
				throw new CompilerException(block,"there is no return in this block and this function doesn't return void");
			}
			if(macroDeclaration.isEmpty()){
                b.write("}\n");
            }
		}else{
		    if(!(env.hasModifier(Modifier.ABSTRACT) || env.hasModifier(Modifier.NATIVE))){
		        throw new CompilerException(line,charNum,"block required");
            }
			appendSemicolon = true;
		}
        FunctionType type;
		var implName = cID;
        if(env.hasModifier(Modifier.ABSTRACT)){
            type = FunctionType.ABSTRACT;
            appendSemicolon = false;
            if(env instanceof ClassEnv e && !e.isAbstract()){
                throw new CompilerException(line,charNum,"abstract method can only be declared in abstract class");
            }
        }else if(env.hasModifier(Modifier.VIRTUAL) || env.hasModifier(Modifier.OVERRIDE)){
            type = FunctionType.VIRTUAL;
            implName = IFunction.toCId(id.getValue());
        }else if(env.hasModifier(Modifier.NATIVE)){
            type = FunctionType.NATIVE;
        }else{
            type = FunctionType.NORMAL;
        }
        var f = new Function(id.getValue(),returnType,cID,args.values().toArray(new TypeInfo[0]), type,self,env.getAccessModifier(),implName,attrCode.toString());
        if(!macroDeclaration.isEmpty()){
            f.setDeclaration(macroDeclaration.toString());
        }
        if(self != null){
            var parentField = self.getField(id.getValue(),env);
            if(env.hasModifier(Modifier.OVERRIDE)){
                if(!(parentField instanceof Function parentMethod)){
                    throw new CompilerException(line,charNum,"there is no method " + id + " to override");
                }
                if(!parentMethod.equalSignature(f)){
                    throw new CompilerException(line,charNum,"this method doesn't have signature of one it overrides");
                }
            }else if(parentField != null){
                throw new CompilerException(line,charNum,"override is required");
            }
            if(env.hasModifier(Modifier.OVERRIDE) || env.hasModifier(Modifier.VIRTUAL)){
                String methodImplName;
                if(env.hasModifier(Modifier.VIRTUAL)){
                    methodImplName = f.getImplName();
                }else{
                    methodImplName = ((Function)parentField).getImplName();
                }
                var cEnv = (ClassEnv)env; //override can only appear in ClassEnv
                cEnv.appendToInitializer(cEnv.getDataName() + "." + methodImplName + "=&");
                cEnv.appendToInitializer(f.getCname() + ";\n");
            }
        }
        var p = new PointerInfo(f);
        if(env instanceof ClassEnv cEnv){
            cEnv.addMethod(f,b.getCode());
        }else if(env instanceof InterfaceEnv){
            writer.write(p.getFunctionPointerDeclaration(cID) + ";\n");
        }else if(env instanceof ModuleEnv e){
            if(f.getType() != FunctionType.NATIVE){
                e.appendFunctionCode(b.getCode());
            }
            e.appendFunctionDeclaration(f);
        }else{
            writer.write(b.getCode());
        }
        env.addFunction("&" + id,new ValueExp(new PointerInfo(f),"&" + cID));
        env.addFunction(id.getValue(),f);
        env.addType(id.getValue(),p,false);
		return TypeInfo.VOID;
	}
	@Override
	public boolean appendSemicolon(){
		return appendSemicolon;
	}
}