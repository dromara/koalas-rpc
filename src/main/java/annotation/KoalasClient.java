package annotation;

import client.proxyfactory.KoalasClientProxy;

import java.lang.annotation.*;

/**
 * Copyright (C) 2019
 * All rights reserved
 * User: yulong.zhang
 * Date:2019年04月17日13:46:40
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface KoalasClient {

    String zkPath() default "";

    String serverIpPorts() default "";

    String genericService() default "";

    boolean cat() default false;

    int connTimeout() default KoalasClientProxy.DEFUAL_CONNTIMEOUT;

    int readTimeout() default KoalasClientProxy.DEFUAL_READTIMEOUT;

    String localMockServiceImpl() default "";

    boolean retryRequest() default true;

    int retryTimes() default 3;

    int maxTotal() default  100;

    int maxIdle() default 50;

    int minIdle() default  10;

    boolean lifo() default true;

    boolean fairness() default  false;

    long maxWaitMillis() default 30 *1000;

    long timeBetweenEvictionRunsMillis() default 3 * 60 * 1000;

    long minEvictableIdleTimeMillis() default  5 * 60 * 1000;

    long softMinEvictableIdleTimeMillis() default 10 * 60 * 1000;

    int numTestsPerEvictionRun() default 20;

    boolean testOnCreate() default  false;

    boolean testOnBorrow() default  false;

    boolean testOnReturn() default  false;

    boolean testWhileIdle() default  true;

    String iLoadBalancer() default "";

    String env() default  "dev";

    boolean removeAbandonedOnBorrow() default  true;

    boolean removeAbandonedOnMaintenance() default  true;

    int removeAbandonedTimeout() default  30;

    int maxLength_() default KoalasClientProxy.DEFUAL_MAXLENGTH;

    String privateKey() default "";

    String publicKey() default "";
}
