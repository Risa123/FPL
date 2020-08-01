package risa.fpl.env;

import risa.fpl.function.AccessModifier;
import risa.fpl.function.ModifierBlockStat;
import risa.fpl.function.SetAccessModifier;
import risa.fpl.function.Use;
import risa.fpl.function.block.ClassBlock;
import risa.fpl.function.block.Fn;
import risa.fpl.function.exp.ValueExp;
import risa.fpl.function.statement.Array;
import risa.fpl.function.statement.Var;
import risa.fpl.info.NumberInfo;
import risa.fpl.info.TypeInfo;

public final class ProgramEnv extends AEnv {
  public ProgramEnv() {
	  addFunction("fn",new Fn());
	  addFunction("native",new ModifierBlockStat(Modifier.NATIVE));
	  addFunction("const",new ModifierBlockStat(Modifier.CONST));
	  addFunction("use",new Use());
	  addFunction("true",new ValueExp(TypeInfo.BOOL,"1"));
	  addFunction("false",new ValueExp(TypeInfo.BOOL,"0"));
	  addFunction("class",new ClassBlock());
	  addFunction("var",new Var(null));
	  addFunction("[]",new Array());
	  addType("void",TypeInfo.VOID,false);
	  addType("byte",NumberInfo.BYTE);
	  addType("sbyte",NumberInfo.SBYTE);
	  addType("ubyte",NumberInfo.UBYTE);
	  addType("ushort",NumberInfo.USHORT);
	  addType("sshort",NumberInfo.SSHORT);
	  addType("short",NumberInfo.SHORT);
	  addType("int",NumberInfo.INT);
	  addType("uint",NumberInfo.UINT);
	  addType("sint",NumberInfo.SINT);
	  addType("long",NumberInfo.LONG);
	  addType("ulong",NumberInfo.ULONG);
	  addType("slong",NumberInfo.SLONG);
	  addType("bool",TypeInfo.BOOL);
	  addType("string",TypeInfo.STRING);
	  addType("float",NumberInfo.FLOAT);
	  addType("double",NumberInfo.DOUBLE);
	  addType("char",TypeInfo.CHAR);
	  addType("memory",NumberInfo.MEMORY);
	  addFunction("nil",new ValueExp(TypeInfo.NIL,"0"));
	  addFunction("private",new SetAccessModifier(AccessModifier.PRIVATE));
  }
}