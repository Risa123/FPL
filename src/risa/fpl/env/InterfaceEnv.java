package risa.fpl.env;

import risa.fpl.CompilerException;
import risa.fpl.function.IFunction;
import risa.fpl.function.block.ExpressionInfo;
import risa.fpl.function.exp.AField;
import risa.fpl.function.statement.Var;
import risa.fpl.info.InterfaceInfo;
import risa.fpl.parser.Atom;

import java.util.ArrayList;

public final class InterfaceEnv extends SubEnv{
    private final InterfaceInfo type;
    private final int firstLine;
    private ArrayList<ExpressionInfo>block;
    public InterfaceEnv(ModuleEnv module,String id,int firstLine){
        super(module);
        this.firstLine = firstLine;
        type = new InterfaceInfo(module,id);
        addModifier(Modifier.ABSTRACT);
    }
    @Override
    public IFunction getFunction(Atom atom)throws CompilerException{
        var f = super.getFunction(atom);
        if(f instanceof Var){
            throw new CompilerException(atom,"variables cannot be declared in interface");
        }
        return f;
    }
    @Override
    public void addFunction(String name,IFunction value){
        type.addField(name,(AField)value);
    }
    public InterfaceInfo getType(){
        return type;
    }
    public int getFirstLine(){
        return firstLine;
    }
    public ArrayList<ExpressionInfo>getBlock(){
        return block;
    }
    public void setBlock(ArrayList<ExpressionInfo>block){
        this.block = block;
    }
}