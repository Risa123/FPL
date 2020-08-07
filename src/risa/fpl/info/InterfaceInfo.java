package risa.fpl.info;

import risa.fpl.function.IFunction;

public final class InterfaceInfo extends TypeInfo {
    public InterfaceInfo(String name) {
        super(name, IFunction.toCId(name) + "*");
    }
}