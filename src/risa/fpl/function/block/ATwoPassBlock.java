package risa.fpl.function.block;

import risa.fpl.BuilderWriter;
import risa.fpl.CompilerException;
import risa.fpl.env.AEnv;
import risa.fpl.parser.AExp;
import risa.fpl.parser.Atom;
import risa.fpl.parser.List;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

public abstract class ATwoPassBlock {
    protected final void compile(BufferedWriter writer, AEnv env,List list) throws CompilerException, IOException {
        var infos = new ArrayList<ExpInfo>(list.getExps().size());
        for(var exp:list.getExps()) {
            var info = new ExpInfo();
            info.exp = exp;
            info.writer = new BuilderWriter(writer);
            infos.add(info);
        }
        for(;;) {
            var noAttempt = false;
            var it = infos.iterator();
            while(it.hasNext()) {
                var info = it.next();
                if(!info.attemptedToCompile) {
                    noAttempt = true;
                }
                try {
                    info.exp.compile(info.writer, env,null);
                    it.remove();
                    writer.write(info.writer.getCode());
                }catch(CompilerException e) {
                    info.attemptedToCompile = true;
                    info.lastEx = e;
                    info.writer = new BuilderWriter(writer);
                    var exps = ((List)info.exp).getExps();
                    if(!exps.isEmpty() && exps.get(0) instanceof Atom a && a.getValue().equals("use")) {
                        throw e;
                    }
                }
            }
            if(!noAttempt) {
                if(!infos.isEmpty()) {
                    var b = new StringBuilder("errors in two pass block:");
                    for(var info:infos) {
                        b.append('\n');
                        info.lastEx.setSourceFile("");
                        b.append(info.lastEx.getMessage());
                    }
                    var first = infos.get(0).exp;
                    throw new CompilerException(first.getLine(),first.getCharNum(),b.toString());
                }
                break;
            }
        }
    }
    private static class ExpInfo{
        AExp exp;
        CompilerException lastEx;
        boolean attemptedToCompile;
        BuilderWriter writer;
    }
}