package monitor;

public class KoalasLocalThreadCtx {
    public static ThreadLocal<KoalasCatCtx> localCtx=new ThreadLocal<>();

    public static void set(KoalasCatCtx ctx){
        localCtx.set ( ctx );
    }

    public static KoalasCatCtx get(){
        return localCtx.get ();
    }

    public static boolean isEmpty(){
        return get()==null;
    }

    public static void remove(){
        localCtx.remove ();
    }
}
