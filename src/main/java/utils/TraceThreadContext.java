package utils;

import protocol.KoalasTrace;
/**
 * Copyright (C) 2019
 * All rights reserved
 * User: yulong.zhang
 * Date:2019年04月17日13:46:40
 */
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
