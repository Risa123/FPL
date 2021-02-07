package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.*;
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
	    var b = new BuilderWriter(writer);
		var returnType = env.getType(it.nextID());
		b.write(returnType.getCname());
		b.write(' ');
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
		b.write(cID);
        TypeInfo self = null;
        if(env instanceof  ClassEnv cEnv){
            self = cEnv.getInstanceType();
        }else if(env instanceof InterfaceEnv e){
            self = e.getType();
        }
        var fnEnv = new FnEnv(env,returnType);
		var args = parseArguments(b,it,fnEnv,self);
		var attrCode = new StringBuilder();
        if(it.hasNext() && it.peek() instanceof Atom a && a.getType() == TokenType.END_ARGS){
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
                b.write(attrCode.toString());
            }
        }
		if(it.hasNext()){
		    if(env.hasModifier(Modifier.ABSTRACT)){
		        throw new CompilerException(line,charNum,"abstract methods can only be declared");
            }
			b.write("{\n");
			var block = it.nextList();
			block.compile(b,fnEnv,it);
			if(fnEnv.notReturnUsed() && returnType != TypeInfo.VOID){
				throw new CompilerException(block,"there is no return in this block and this function doesn't return void");
			}
			b.write("}\n");
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
        var f = new Function(id.getValue(),returnType,cID,args,type,self,env.getAccessModifier(),implName,attrCode.toString());
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