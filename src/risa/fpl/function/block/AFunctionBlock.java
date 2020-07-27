package risa.fpl.function.block;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import risa.fpl.CompilerException;
import risa.fpl.env.FnEnv;
import risa.fpl.function.IFunction;
import risa.fpl.function.exp.Variable;
import risa.fpl.info.TypeInfo;
import risa.fpl.parser.ExpIterator;
import risa.fpl.parser.List;

public abstract class AFunctionBlock extends ABlock {
  TypeInfo[]parseArguments(BufferedWriter writer,ExpIterator it,FnEnv env) throws CompilerException, IOException{
	  var args = new ArrayList<TypeInfo>();
	  var first = true;
	  while(it.hasNext()) {
		  if(it.peek() instanceof List) {
			  break;
		  }
		  if(first) {
			  first = false;
		  }else {
			  writer.write(',');
		  }
		  var argType = env.getType(it.nextID());
		  writer.write(argType.cname);
		  writer.write(' ');
		  args.add(argType);
		  var argName = it.nextID();
		  writer.write(IFunction.toCId(argName.value));
		  if(env.hasFunctionInCurrentEnv(argName.value)) {
			  throw new CompilerException(argName,"there is already argument called " + argName);
		  }
		  env.addFunction(argName.value, new Variable(argType,IFunction.toCId(argName.value),argName.value));
	  }
	  var array = new TypeInfo[args.size()];
	  args.toArray(array);
	  return array;
  }
}