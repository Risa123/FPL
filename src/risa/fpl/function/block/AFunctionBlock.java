package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.FnEnv;
import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.InstanceInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.PointerInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.util.LinkedHashMap;

public abstract class AFunctionBlock extends ABlock{
 protected final LinkedHashMap<String,TypeInfo>parseArguments(StringBuilder builder,ExpIterator it,FnEnv env,TypeInfo owner)throws CompilerException{
        builder.append('(');
        var args = new LinkedHashMap<String,TypeInfo>();
        var first = owner == null;
        if(!first){
            builder.append(owner.getCname()).append("* this");
            env.addFunction("this",new Variable(new PointerInfo(owner),"this","this"));
            if(owner.getPrimaryParent() != null){
                env.addFunction("super",new Variable(new PointerInfo(owner.getPrimaryParent()),"this","super"));
            }
        }
        while(it.hasNext()){
            var peeked = it.peek();
            if(peeked instanceof List || ((Atom)peeked).getValue().equals("=") || ((Atom)peeked).getType() == AtomType.CLASS_SELECTOR){
                break;
            }
            if(first){
                first = false;
            }else{
                builder.append(',');
            }
            var argTypeAtom = it.nextID();
            var argType = env.getType(argTypeAtom);
            var argName = it.nextAtom();
            var constant = false;
            if(argName.getType() == AtomType.END_ARGS){
                argType = generateTypeFor(argType,argTypeAtom,it,env,false);
                argName = it.nextID();
                if(argName.getValue().equals("*")){
                    argType = new PointerInfo(argType);
                    argName = it.nextID();
                }
            }else if(argName.getType() != AtomType.ID){
                error(argName,"identifier or ; expected");
            }else if(argName.getValue().equals("const")){
                argName = it.nextID();
                constant = true;
            }
            if(args.containsKey(argName.getValue())){
                error(argName,"there is already argument called " + argName);
            }
            args.put(argName.getValue(),argType);
            var argNameCID = IFunction.toCId(argName.getValue());
            if(constant && argType instanceof PointerInfo p){
                p.makeConstant();
            }
            builder.append(argType.getCname()).append(' ').append(argNameCID);
            var v = new Variable(argType,argNameCID,false,argName.getValue(),constant,null,AccessModifier.PUBLIC);
            env.addFunction(argName.getValue(),v);
            if(argType instanceof InstanceInfo i){
                env.addInstanceVariable(i,argNameCID);
            }else if(argType instanceof InterfaceInfo i){
                env.addInterfaceFreeCall(i.getDestructorName(),argNameCID);
            }
        }
        builder.append(')');
        return args;
    }
}