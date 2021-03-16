package risa.fpl.function.block;

import risa.fpl.function.IFunction;

public abstract class ABlock implements IFunction{
 @Override
 public boolean appendSemicolon(){
	 return false;
 }
}