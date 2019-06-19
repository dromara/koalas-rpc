package thrift.annotation.server.impl;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class KoalasAop {

    @Pointcut("execution(public * thrift.annotation.server.impl.*.*(..))")
    public void myMethod(){}

    @Before("myMethod()")
    public void before1(JoinPoint joinPoint) throws NoSuchMethodException {
        System.out.println ("joinPoint.getTarget () "+joinPoint.getTarget ());
        System.out.println ("joinPoint.getThis () "+joinPoint.getThis ());
        System.out.println ("joinPoint.getThis ()==joinPoint.getTarget () "+joinPoint.getThis ()==joinPoint.getTarget ());

        System.out.println ("joinPoint.getThis ().getClass ().getName () "+joinPoint.getThis ().getClass ().getName ());
        System.out.println ("joinPoint.getTarget ().getClass ().getName () "+joinPoint.getTarget ().getClass ().getName ());

        System.out.println ("joinPoint.getThis ().getClass () "+joinPoint.getThis ().getClass ());
        System.out.println ("joinPoint.getTarget ().getClass() "+joinPoint.getTarget ().getClass());



        System.out.println ("AopUtils.isJdkDynamicProxy ( joinPoint.getThis () )  " + AopUtils.isJdkDynamicProxy ( joinPoint.getThis () ) );
        System.out.println ("AopUtils.isJdkDynamicProxy ( joinPoint.getTarget () ) "+AopUtils.isJdkDynamicProxy ( joinPoint.getTarget () ) );
        System.out.println ("AopUtils.isCglibProxy ( joinPoint.getThis () ) "+AopUtils.isCglibProxy ( joinPoint.getThis () ) );
        System.out.println ("AopUtils.isCglibProxy ( joinPoint.getTarget () ) "+AopUtils.isCglibProxy ( joinPoint.getTarget () ) );

        System.out.println ("joinPoint.getThis ().getClass().getSuperclass () "+joinPoint.getThis ().getClass().getSuperclass ());
        System.out.println ("joinPoint.getTarget ().getClass().getSuperclass () "+joinPoint.getTarget ().getClass().getSuperclass ());

        MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature ();
        System.out.println ("methodSignature.getMethod () "+methodSignature.getMethod ());


        System.out.println("method start1");
    }
}
