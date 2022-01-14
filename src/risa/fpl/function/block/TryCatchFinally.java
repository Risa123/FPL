package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.FnSubEnv;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.util.ArrayList;

public final class TryCatchFinally extends ABlock{
    private static InstanceInfo exception;
    @SuppressWarnings("ConstantConditions")
    @Override
    public TypeInfo compile(StringBuilder  builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        env.checkModifiers(line,tokenNum);
        var postEntry = new StringBuilder("{\nchar exceptionCaught = 0;\n");
        postEntry.append("if(!__builtin_setjmp(_std_lang_currentThread->_currentEHEntry->_context)){\n");
        var tryEnv = new FnSubEnv(env);
        var tmp = new StringBuilder();
        it.nextList().compile(tmp,tryEnv,it);
        tryEnv.compileToPointerVars(builder);
        postEntry.append(tmp).append("}\n");
        var hasFin = false;
        var finallyCode = "";
        var finallyEnv = new FnSubEnv(env);
        var exDataNames = new ArrayList<String>();
        if(exception == null){
            exception = (InstanceInfo)env.getType(new Atom(0,0,"Exception", AtomType.ID));
        }
        while(it.hasNext()){
            if(it.peek() instanceof Atom blockName && blockName.getType() == AtomType.ID){
                if(blockName.getValue().equals("catch")){
                    if(hasFin){
                        throw new CompilerException(blockName,"catch can only come before finally");
                    }
                    it.next();
                    List block;
                    postEntry.append("else");
                    var nextExp = it.next();
                    InstanceInfo exInfo;
                    if(nextExp instanceof List){
                        block = (List)nextExp;
                        exInfo = exception;
                    }else if(nextExp instanceof Atom exType && exType.getType() == AtomType.ID){
                        if(exType.getValue().equals("Exception")){
                            throw new CompilerException(exType,"unnecessary Exception ID");
                        }
                        postEntry.append(" if(_std_lang_currentThread->_exception->objectData==&");
                        if(env.getType(exType) instanceof InstanceInfo i){
                            exInfo = i;
                        }else{
                            throw new CompilerException(exType,"instance type expected");
                        }
                        postEntry.append(exInfo.getDataName()).append(")");
                        block = it.nextList();
                        exDataNames.add(exInfo.getDataName());
                    }else{
                        throw new CompilerException(nextExp,"exception type or block expected");
                    }
                    if(!exInfo.isException()){
                        throw new CompilerException(nextExp,"invalid exception");
                    }
                    postEntry.append("{\n_std_lang_Thread_removeEHEntry0(_std_lang_currentThread);\n");
                    postEntry.append("exceptionCaught = 1;\n");
                    postEntry.append(exInfo.getCname());
                    postEntry.append(" ex;\n_std_lang_Exception_copyAndFree0(_std_lang_currentThread->_exception,(_Exception*)&ex);\n");
                    var blockEnv = new FnSubEnv(env);
                    blockEnv.addFunction("ex",new Variable(exInfo,"ex","ex"));
                    var tmp1 = new StringBuilder();
                    block.compile(tmp1,blockEnv,it);
                    blockEnv.compileToPointerVars(builder);
                    postEntry.append(tmp1);
                    postEntry.append("}\n");
                }else if(blockName.getValue().equals("finally")){
                    if(hasFin){
                        throw new CompilerException(blockName,"multiple declarations of finally");
                    }
                    hasFin = true;
                    it.next();
                    var b = new StringBuilder();
                    it.nextList().compile(b,finallyEnv,it);
                    finallyCode = b.toString();
                }else{
                    break;
                }
            }else{
                break;
            }
        }
        postEntry.append("if(!exceptionCaught){\n_std_lang_Thread_removeEHEntry0(_std_lang_currentThread);\n}\n");
        finallyEnv.compileToPointerVars(postEntry);
        postEntry.append(finallyCode);
        finallyEnv.compileDestructorCalls(postEntry);
        postEntry.append("}\n");
        if(exDataNames.isEmpty()){
            exDataNames.add(exception.getDataName());
        }
        builder.append("{\nvoid* types[").append(exDataNames.size()).append("]={");
        var first = true;
        for(var name:exDataNames){
            if(first){
                first = false;
            }else{
                builder.append(',');
            }
            builder.append("&").append(name);
        }
        builder.append("};\n_std_lang_Thread_addEHEntry0(_std_lang_currentThread,types,").append(exDataNames.size()).append(");\n}\n");
        builder.append(postEntry);
        return TypeInfo.VOID;
    }
}