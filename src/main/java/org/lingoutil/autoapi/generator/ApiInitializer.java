package org.lingoutil.autoapi.generator;

import jakarta.annotation.Resource;
import org.lingoutil.autoapi.annotation.GenerateApi;
import org.lingoutil.autoapi.config.ApiConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;

@Component
public class ApiInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApiInitializer.class);

    @Resource
    private ApplicationContext context;

    @Resource
    private ApiGenerator apiGenerator;

    @Resource
    private ApiConfiguration apiConfiguration;

    public void initialize() {
        Boolean enable = apiConfiguration.getEnable();
        if (!enable) {
            return;
        }

        String[] controllerBeanNames = context.getBeanNamesForAnnotation(Controller.class);

        ArrayList<Class<?>> classArrayList = new ArrayList<>();
        for (String controllerBeanName : controllerBeanNames) {
            logger.info("autoAPI found controller ：{} ", controllerBeanName);
            Class<?> aClass = context.getBean(controllerBeanName).getClass();

            // 判断该类是否有GenerateApi注解且enable属性为true，是则加入list
            if (aClass.isAnnotationPresent(GenerateApi.class) && aClass.getAnnotation(GenerateApi.class).enabled()) {
                classArrayList.add(aClass);
            }
        }

        apiGenerator.generateApiCode(classArrayList);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initialize();
    }
}
