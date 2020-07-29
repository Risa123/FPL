package risa.fpl.function.exp;

import java.io.BufferedWriter;
import java.io.IOException;

public abstract class AField implements IField {
    private String prev_code;
    @Override
    public void setPrevCode(String code) {
        prev_code = code;
    }
    @Override
    public void writePrev(BufferedWriter writer) throws IOException {
        if(prev_code != null){
            writer.write(prev_code);
            prev_code = null;
        }
    }
    @Override
    public String getPrevCode() {
        return prev_code;
    }
}