package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.env.FnEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.exp.Function;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.tokenizer.TokenType;

import java.io.BufferedWriter;
import java.io.IOException;

public final class TryCatchFinally extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int tokenNum)throws IOException,CompilerException{
        writer.write("_std_lang_Thread_addEHentry0(_std_lang_currentThread);\n");
        var backend = env.getFPL().getModule("std.backend").getEnv();
        writer.write(((Function)backend.getFunction(new Atom(0,0,"contextSave",TokenType.ID))).getDeclaration());
        writer.write("if(!_std_backend_contextSave0(_std_lang_currentThread->_currentEHentry->_context)){\n");
        var tryEnv = new FnSubEnv(env);
        var tmp = new BuilderWriter(writer);
        it.nextList().compile(tmp,tryEnv,it);
        tryEnv.compileToPointerVars(writer);
        writer.write(tmp.getCode());
        writer.write("}\n");
        var hasFin = false;
        var finallyCode = "";
        var finallyEnv = new FnSubEnv(env);
        while(it.hasNext()){
            var exp = it.peek();
            if(exp instanceof Atom blockName && blockName.getType() == TokenType.ID){
                if(blockName.getValue().equals("catch")){
                    if(hasFin){
                        throw new CompilerException(exp,"catch can only come before finally");
                    }
                    it.next();
                    List block;
                    writer.write("else");
                    var nextExp = it.next();
                    TypeInfo exInfo;
                    var exception = env.getType(new Atom(0,0,"Exception",TokenType.ID));
                    if(nextExp instanceof List){
                        block = (List)nextExp;
                        exInfo = exception;
                    }else if(nextExp instanceof Atom exType && exType.getType() == TokenType.ID){
                        if(exType.getValue().equals("Exception")){
                            throw new CompilerException(exType,"unnecessary Exception ID");
                        }
                        writer.write(" if(_std_lang_currentThread->_exception->object_data==&");
                        exInfo = env.getType(exType);
                        writer.write(exInfo.getCname());
                        writer.write("_data)");
                        block = it.nextList();
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
                    writer.write("{\n");
                    writer.write(exInfo.getCname());
                    writer.write(" ex;\n_std_lang_Exception_copyAndFree0(_std_lang_currentThread->_exception,&ex);\n");
                    var blockEnv = new FnSubEnv(env);
                    blockEnv.addFunction("ex",new Variable(exInfo,"ex","ex"));
                    var tmp1 = new BuilderWriter(writer);
                    block.compile(tmp1,blockEnv,it);
                    blockEnv.compileToPointerVars(writer);
                    writer.write(tmp1.getCode());
                    writer.write("}\n");
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
        writer.write("{\n_std_lang_Thread_removeEHentry0(_std_lang_currentThread);\n");
        finallyEnv.compileToPointerVars(writer);
        writer.write(finallyCode);
        finallyEnv.compileDestructorCalls(writer);
        writer.write("}\n");
        return TypeInfo.VOID;
    }
}