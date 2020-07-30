package risa.fpl.info;

import risa.fpl.function.exp.BinaryOperator;
import risa.fpl.function.exp.Cast;
import risa.fpl.function.exp.Size;
import risa.fpl.function.exp.UnaryOperator;

public final class NumberInfo extends TypeInfo {
	public static final NumberInfo BYTE = new NumberInfo("byte","char",1);
	public static final NumberInfo UBYTE = new NumberInfo("ubyte","unsigned char",1);
	public static final NumberInfo SBYTE = new NumberInfo("sbyte","signed char",1);
	public static final NumberInfo SHORT = new NumberInfo("short","short",2);
	public static final NumberInfo SSHORT = new NumberInfo("sshort","signed short",2);
	public static final NumberInfo USHORT = new NumberInfo("ushort","unsigned short",2);
	public static final NumberInfo INT = new NumberInfo("int","int",4);
	public static final NumberInfo UINT = new NumberInfo("uint","unsigned int",4);
	public static final NumberInfo SINT = new NumberInfo("sint","signed int",4);
	public static final NumberInfo LONG = new NumberInfo("long","long",8);
	public static final NumberInfo SLONG = new NumberInfo("slong","signed long",8);
	public static final NumberInfo ULONG = new NumberInfo("ulong","unsigned long",8);
	public static final NumberInfo FLOAT = new NumberInfo("float","float",4,true);
	public static final NumberInfo DOUBLE = new NumberInfo("double","double",8,true);
	public static final NumberInfo MEMORY = new NumberInfo("memory","unsigned long",8);
    private final int size;
    public final boolean floatingPoint;
	public NumberInfo(String name, String cname,int size,boolean floatingPoint) {
		super(name, cname);
		this.size = size;
		this.floatingPoint = floatingPoint;
		addField("==",new BinaryOperator(TypeInfo.BOOL,this,"=="));
		addField("!=",new BinaryOperator(TypeInfo.BOOL,this,"!="));
		addField("+",new BinaryOperator(this,this,"+"));
		addField("-",new BinaryOperator(this,this,"-"));
		addField("/",new BinaryOperator(this,this,"/"));
		addField("*",new BinaryOperator(this,this,"*"));
		addField("++",new UnaryOperator(this,"++",true));
		addField("--",new UnaryOperator(this,"--",true));
		addField("p+",new UnaryOperator(this,"++",false));
		addField("p-",new UnaryOperator(this,"--",false));
		addField("cast",new Cast(this));
	}
	public NumberInfo(String name,String cname,int size) {
		this(name,cname,size,false);
		addField("%",new BinaryOperator(this,this,"%"));
	}
	@Override
	public boolean equals(Object o) {
		if(o instanceof NumberInfo n) {
			if(!floatingPoint && n.floatingPoint) {
				return false;
			}
			return size >= n.size;
		}
		return super.equals(o);
	}
}