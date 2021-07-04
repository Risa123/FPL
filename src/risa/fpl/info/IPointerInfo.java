package risa.fpl.info;

public interface IPointerInfo {
    String getPointerVariableDeclaration(String cID);
    FunctionInfo getFunctionPointer();
    int getFunctionPointerDepth();
}