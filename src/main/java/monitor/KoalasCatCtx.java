package monitor;

import com.dianping.cat.Cat;

public class KoalasCatCtx implements Cat.Context {
    private KoalasContext KoalasContext;
    public KoalasContext getKoalasContext() {
        return KoalasContext;
    }
    public void setKoalasContext(KoalasContext KoalasContext) {
        this.KoalasContext = KoalasContext;
    }
    @Override
    public void addProperty(String key, String value) {
        if(KoalasContext ==null){
            KoalasContext =new KoalasContext (  );
        }
        switch (key){
            case "_catRootMessageId":
                KoalasContext.setCatRootMessageId ( value );break;
            case "_catParentMessageId":
                KoalasContext.setCatParentMessageId ( value );break;
            case "_catChildMessageId":
                KoalasContext.setCatChildMessageId ( value );break;
        }
    }

    @Override
    public String getProperty(String key) {
        switch (key){
            case "_catRootMessageId":return KoalasContext.getCatRootMessageId();
            case "_catParentMessageId":return KoalasContext.getCatParentMessageId ();
            case "_catChildMessageId":return KoalasContext.getCatChildMessageId ();
        }
        return null;
    }
}
