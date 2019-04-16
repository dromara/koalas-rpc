package utils;

import protocol.KoalasTrace;

public class TraceThreadContext {
    private static ThreadLocal<KoalasTrace> localTraceContest = new ThreadLocal<>();

    public static void set(KoalasTrace koalasTrace){
        localTraceContest.set ( koalasTrace );
    }

    public static KoalasTrace get(){
        return localTraceContest.get ();
    }

    public static void remove(){
        localTraceContest.remove ();
    }
}
