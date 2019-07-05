package annotation;

import java.lang.annotation.*;

/**
 * Copyright (C) 2019
 * All rights reserved
 * User: yulong.zhang
 * Date:2019年04月17日13:46:40
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface KoalasServer {
    int port();
    String zkpath() default "";
    boolean cat() default false;
    int bossThreadCount() default 0;
    int workThreadCount() default  0;
    int koalasThreadCount() default 0;
    int maxLength() default Integer.MAX_VALUE;
    String env() default "dev";
    int weight() default  10;
    String serverType() default  "NETTY";
    int workQueue() default 0;
    String privateKey() default "";
    String publicKey() default "";
}
