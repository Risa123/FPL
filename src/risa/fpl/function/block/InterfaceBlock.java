package risa.fpl.function.block;

import risa.fpl.CompilerException;
import risa.fpl.env.SubEnv;
import risa.fpl.env.InterfaceEnv;
import risa.fpl.env.ModuleEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.FunctionType;
import risa.fpl.info.FunctionInfo;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.Atom;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;
import risa.fpl.parser.AtomType;

import java.util.ArrayList;

public final class InterfaceBlock extends AThreePassBlock implements IFunction{
    @SuppressWarnings("ConstantConditions")
    @Override
    public TypeInfo compile(StringBuilder builder,SubEnv env,ExpIterator it,int line,int tokenNum)throws CompilerException{
        if(!(env instanceof ModuleEnv mod)){
            throw new CompilerException(line,tokenNum,"interface can only be declared on module level");
        }
        env.checkModifiers(line,tokenNum);
        var id = it.nextID();
        var idV = id.getValue();
        InterfaceEnv iEnv = null;
        for(var e:mod.getInterfaceEnvList()){
            if(e.getType().getName().equals(idV)){
                iEnv = e;
            }
        }
        if(iEnv == null){
            iEnv = new InterfaceEnv(mod,idV,line);
            mod.getInterfaceEnvList().add(iEnv);
        }
        if(env.hasTypeInCurrentEnv(idV) && (!(env.getType(id) instanceof InterfaceInfo) || iEnv.getFirstLine() != line)){
            error(id,"this type " + idV + " is already declared");
        }
        var type = iEnv.getType();
        var cID = type.getCname();
        env.addType(type);
        List block = null;
        while(it.hasNext()){
            var exp = it.next();
            if(exp instanceof List l){
                block = l;
                break;
            }else{
               var typeID = (Atom)exp;
               if(typeID.getType() != AtomType.ID){
                   error(id,"identifier expected");
               }
               if(!(env.getType(typeID) instanceof InterfaceInfo parentType)){
                   throw new CompilerException(typeID,"interface can only inherit from interfaces");
               }
               type.addParent(parentType);
            }
        }
        if(block == null){
            throw new CompilerException(line,tokenNum,"block expected as last argument");
        }
        ArrayList<ExpressionInfo>infos;
        if(iEnv.getBlock() == null){
            infos = createInfoList(block);
            iEnv.setBlock(infos);
        }else{
            infos = iEnv.getBlock();
        }
        compile(builder,iEnv,infos);
        var implName = type.getImplName();
        var b = new StringBuilder("typedef struct ").append(implName).append("{\n");
        for(var entry:type.getMethodVariantsOfType(FunctionType.ABSTRACT).entrySet()){
            var p = new FunctionInfo(entry.getValue());
            b.append(p.getPointerVariableDeclaration(entry.getKey().getCname())).append(";\n");
        }
        b.append("void(*copyConstructor)(void*,void*);\n").append(NumberInfo.MEMORY.getCname()).append(" instanceSize;\n");
        b.append("void(*destructor)(void*);\n");
        b.append('}').append(implName).append(";\n");
        b.append("typedef struct ").append(cID).append("{\nvoid* instance;\n");
        b.append(implName).append("* impl;\n}").append(cID).append(";\n");
        b.append("void ").append(type.getCopyName()).append('(').append(cID).append("*,").append(cID).append("*);\n");
        b.append("void ").append(type.getDestructorName()).append('(').append(cID).append("*);\n");
        b.append(cID).append(' ').append(type.getCopyName()).append("AndReturn(").append(cID).append(");\n");
        type.appendToDeclaration(b.toString());
        type.buildDeclaration();
        mod.appendFunctionCode("void " + type.getCopyName() + '(');
        mod.appendFunctionCode(cID + "* this," + cID + "* o){\nthis->impl=o->impl;\n");
        mod.appendFunctionCode("void* malloc(" + NumberInfo.MEMORY.getCname() + ");\n");
        mod.appendFunctionCode("this->instance=malloc(this->impl->instanceSize);\n");
        mod.appendFunctionCode("if(this->impl->copyConstructor==0){\n");
        mod.appendFunctionCode("this->instance=o->instance;\n}else{\n");
        mod.appendFunctionCode("this->impl->copyConstructor(this->instance,o->instance);\n}\n}\n");
        mod.appendFunctionCode(cID + ' ' + type.getCopyName() + "AndReturn(" + cID + " original){\n");
        mod.appendFunctionCode(cID + " instance;\n");
        mod.appendFunctionCode(type.getCopyName() + "(&instance,&original);\nreturn instance;\n}\n");
        mod.appendFunctionCode("void " + type.getDestructorName() + '(' + type.getCname() + "* this){\n");
        mod.appendFunctionCode("if(this->impl->destructor!=0){\nthis->impl->destructor(this->instance);\nfree(this->instance);\n}\n}\n");
        return TypeInfo.VOID;
    }
}