package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

import risa.fpl.function.AccessModifier;
import risa.fpl.function.IFunction;

public interface IField extends IFunction{
    void setPrevCode(String code);
    void writePrev(BufferedWriter writer)throws IOException;
    String getPrevCode();
    AccessModifier getAccessModifier();
}