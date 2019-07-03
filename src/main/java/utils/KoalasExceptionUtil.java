package utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class KoalasExceptionUtil {

    public static String getExceptionInfo(Exception e){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(out);
        e.printStackTrace(pout);
        String ret = new String(out.toByteArray());
        pout.close();
        try {
            out.close();
        } catch (Exception ex) {
        }
        return ret;
    }
}
