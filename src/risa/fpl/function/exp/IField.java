package risa.fpl.function.exp;

import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;

public interface IField extends IFunction{
    void setPrevCode(String code);
    AccessModifier getAccessModifier();
}