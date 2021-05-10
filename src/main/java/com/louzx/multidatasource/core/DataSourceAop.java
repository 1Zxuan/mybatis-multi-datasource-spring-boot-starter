package com.louzx.multidatasource.core;


import com.louzx.multidatasource.annotation.DataSource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;

@Aspect
@Component
public class DataSourceAop {

    @Resource
    private DataSourceRouting dataSourceRouting;

    @Pointcut("@annotation(com.louzx.multidatasource.annotation.DataSource)")
    public void annotationDataSourcePointcut(){
    }

    @Pointcut("this(com.louzx.multidatasource.annotation.MultiDataSource)")
    public void interfaceDataSourcePointcut() {}

    @Before("annotationDataSourcePointcut()")
    public void before(JoinPoint joinPoint){
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            Method method = methodSignature.getMethod();
            DataSource dataSource = method.getAnnotation(DataSource.class);
            if (null != dataSource) {
                dataSourceRouting.change(dataSource.value());
            }
        } else {
            Class<?> aClass = joinPoint.getTarget().getClass();
            DataSource annotation = aClass.getAnnotation(DataSource.class);
            if (null != annotation) {
                dataSourceRouting.change(annotation.value());
            }
        }
    }

    @Before("interfaceDataSourcePointcut()")
    public void interfaceBefore(JoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        if (signature instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) signature;
            DataSource dataSource = methodSignature.getMethod().getAnnotation(DataSource.class);
            if (null != dataSource) {
                //方法已有DataSource注解
                return;
            }
        }
        Class<?>[] interfaces = joinPoint.getTarget().getClass().getInterfaces();
        for (Class<?> anInterface : interfaces) {
            DataSource dataSource = anInterface.getAnnotation(DataSource.class);
            if (null != dataSource) {
                dataSourceRouting.change(dataSource.value());
                break;
            }
        }
    }

    @After("interfaceDataSourcePointcut() || annotationDataSourcePointcut()")
    public void after() {
        dataSourceRouting.remove();
    }

}

