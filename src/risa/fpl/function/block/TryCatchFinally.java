package risa.fpl.function.block;

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

public final class TryCatchFinally extends ABlock{
    @Override
    public TypeInfo compile(BufferedWriter writer,AEnv env,ExpIterator it,int line,int charNum)throws IOException,CompilerException{
        writer.write("if(!_std_backend_contextSave(_std_lang_currentThread->_currentEHentry->_context)){\n");
        it.nextList().compile(writer,new FnSubEnv(env),it);
        writer.write("}\n");
        var hasFin = false;
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
                    if(exception.equals(exInfo)){
                        throw new CompilerException(nextExp,"invalid exception");
                    }
                    writer.write("{\n");
                    writer.write(exInfo.getCname());
                    writer.write(" ex;\nmemcpy(&ex,_std_lang_currentThread->_exception,sizeof(");
                    writer.write(exInfo.getCname());
                    writer.write("));\n");
                    writer.write("free(_std_lang_currentThread->_exception);\n");
                    var blockEnv = new FnSubEnv(env);
                    blockEnv.addFunction("ex",new Variable(exInfo,"ex","ex"));
                    block.compile(writer,blockEnv,it);
                    writer.write("}\n");
                }else if(blockName.getValue().equals("finally")){
                    if(hasFin){
                        throw new CompilerException(blockName,"multiple declarations of finally");
                    }
                    hasFin = true;
                    it.next();
                    it.nextList().compile(writer,new FnSubEnv(env),it);
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        return TypeInfo.VOID;
    }
}