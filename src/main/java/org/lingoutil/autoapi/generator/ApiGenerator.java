package org.lingoutil.autoapi.generator;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ApiGenerator {

    @Resource
    private ApiMethodBuilder apiMethodBuilder;

    /**
     * 根据类文件生成该类的接口的API包
     *
     * @param classes 含有{@link org.lingoutil.autoapi.annotation.GenerateApi}注解的Controller类
     */
    public void generateApiCode(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            apiMethodBuilder.execute(clazz);
        }
    }
}
