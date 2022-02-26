package risa.fpl.env;

import risa.fpl.function.*;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.function.block.Fn;
import risa.fpl.function.block.InterfaceBlock;
import risa.fpl.function.exp.ParenthExp;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.statement.*;
import risa.fpl.function.statement.Enum;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;

public final class ProgramEnv extends AEnv{
  public ProgramEnv(){
      addFunction("nil",new ValueExp(TypeInfo.NIL,"0"));
      addFunction("private",new SetAccessModifier(AccessModifier.PRIVATE));
      addFunction("abstract",new AddModifier(Modifier.ABSTRACT));
      addFunction("final",new AddModifier(Modifier.FINAL));
      addFunction("interface",new InterfaceBlock());
      addFunction("fpointer",new FPointer());
      addFunction("struct",new ClassBlock(true));
      addFunction("compIf",new CompileTimeIf());
      addFunction("asm",new Asm());
      addFunction("typedef",new Typedef());
      addFunction("fn",new Fn());
      addFunction("native",new AddModifier(Modifier.NATIVE));
      addFunction("const",new AddModifier(Modifier.CONST));
      addFunction("use",new Use());
      addFunction("true",new ValueExp(TypeInfo.BOOL,"1"));
      addFunction("false",new ValueExp(TypeInfo.BOOL,"0"));
      addFunction("class",new ClassBlock(false));
      addFunction("var",new Var(null));
      addFunction("[]",new Array());
      addFunction("alias",new Alias());
      addFunction("enum",new Enum());
      addFunction("[",new ParenthExp());
      addType(TypeInfo.VOID,false);
	  addType(NumberInfo.BYTE);
	  addType(NumberInfo.SBYTE);
	  addType(NumberInfo.UBYTE);
	  addType(NumberInfo.USHORT);
	  addType(NumberInfo.SSHORT);
	  addType(NumberInfo.SHORT);
	  addType(NumberInfo.INT);
	  addType(NumberInfo.UINT);
	  addType(NumberInfo.SINT);
	  addType(NumberInfo.LONG);
	  addType(NumberInfo.ULONG);
	  addType(NumberInfo.SLONG);
	  addType(TypeInfo.BOOL);
	  addType(NumberInfo.FLOAT);
	  addType(NumberInfo.DOUBLE);
	  addType(TypeInfo.CHAR);
	  addType(NumberInfo.MEMORY);
      addType(NumberInfo.NUMBER,false);
  }
}