package annotation;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface KoalasServer {
    int port();
    String zkpath() default "";
    int bossThreadCount() default 0;
    int workThreadCount() default  0;
    int koalasThreadCount() default 0;
    String env() default "dev";
    int weight() default  10;
    String serverType() default  "NETTY";
    int workQueue() default 0;
    String privateKey() default "";
    String publicKey() default "";
}
