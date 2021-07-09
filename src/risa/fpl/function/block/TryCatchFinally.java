package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class TryCatchFinally extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        var postEntry = new BuilderWriter(writer);
        postEntry.write("if(!");
        var arch = System.getProperty("os.arch");
        switch(arch){
            case "x86"->postEntry.write("_setjmp3");
            case "ia64"->postEntry.write("__mingw_setjmp");
            case "amd64"->postEntry.write("_setjmp");
        }
        postEntry.write("(_std_lang_currentThread->_currentEHEntry->_context");
        switch(arch){
            case "x86"->postEntry.write(",nil");
            case "amd64"->postEntry.write(",__builtin_frame_address(0)");
        }
        postEntry.write(")){\n");
        var tryEnv = new FnSubEnv(env);
        var tmp = new BuilderWriter(writer);
        it.nextList().compile(tmp,tryEnv,it);
        tryEnv.compileToPointerVars(writer);
        postEntry.write(tmp.getCode());
        postEntry.write("}\n");
        var hasFin = false;
        var finallyCode = "";
        var finallyEnv = new FnSubEnv(env);
        var exDataNames = new ArrayList<String>();
        var exception = env.getType(new Atom(0,0,"Exception",TokenType.ID));
        while(it.hasNext()){
            var exp = it.peek();
            if(exp instanceof Atom blockName && blockName.getType() == TokenType.ID){
                if(blockName.getValue().equals("catch")){
                    if(hasFin){
                        throw new CompilerException(exp,"catch can only come before finally");
                    }
                    it.next();
                    List block;
                    postEntry.write("else");
                    var nextExp = it.next();
                    TypeInfo exInfo;
                    if(nextExp instanceof List){
                        block = (List)nextExp;
                        exInfo = exception;
                    }else if(nextExp instanceof Atom exType && exType.getType() == TokenType.ID){
                        if(exType.getValue().equals("Exception")){
                            throw new CompilerException(exType,"unnecessary Exception ID");
                        }
                        postEntry.write(" if(_std_lang_currentThread->_exception->objectData==&");
                        exInfo = env.getType(exType);
                        postEntry.write(exInfo.getClassInfo().getDataName() + ")");
                        block = it.nextList();
                        exDataNames.add(exInfo.getClassInfo().getDataName());
                    }else{
                        throw new CompilerException(nextExp,"exception type or block expected");
                    }
                    var notException = true;
                    var current = exInfo;
                    while(current != null){
                        if(current == exception){
                            notException = false;
                            break;
                        }
                        current = current.getPrimaryParent();
                    }
                    if(notException){
                        throw new CompilerException(nextExp,"invalid exception");
                    }
                    postEntry.write("{\n");
                    postEntry.write(exInfo.getCname());
                    postEntry.write(" ex;\n_std_lang_Exception_copyAndFree0(_std_lang_currentThread->_exception,&ex);\n");
                    var blockEnv = new FnSubEnv(env);
                    blockEnv.addFunction("ex",new Variable(exInfo,"ex","ex"));
                    var tmp1 = new BuilderWriter(writer);
                    block.compile(tmp1,blockEnv,it);
                    blockEnv.compileToPointerVars(writer);
                    postEntry.write(tmp1.getCode());
                    postEntry.write("}\n");
                }else if(blockName.getValue().equals("finally")){
                    if(hasFin){
                        throw new CompilerException(blockName,"multiple declarations of finally");
                    }
                    hasFin = true;
                    it.next();
                    var b = new BuilderWriter(writer);
                    it.nextList().compile(b,finallyEnv,it);
                    finallyCode = b.getCode();
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        postEntry.write("{\n_std_lang_Thread_removeEHEntry0(_std_lang_currentThread);\n");
        finallyEnv.compileToPointerVars(postEntry);
        postEntry.write(finallyCode);
        finallyEnv.compileDestructorCalls(postEntry);
        postEntry.write("}\n");
        if(exDataNames.isEmpty()){
            exDataNames.add(exception.getClassInfo().getDataName());
        }
        writer.write("{\nvoid* types[" + exDataNames.size() + "] = {");
        var first = true;
        for(var name:exDataNames){
            writer.write("&" + name);
            if(first){
                first = false;
            }else{
                writer.write(',');
            }
        }
        writer.write("};\n");
        writer.write("_std_lang_Thread_addEHEntry0(_std_lang_currentThread,types," + exDataNames.size() + ");\n");
        writer.write("}\n");
        writer.write(postEntry.getCode());
        return TypeInfo.VOID;
    }
}