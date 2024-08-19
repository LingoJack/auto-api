package org.lingoutil.autoapi.generator;

import jakarta.annotation.Resource;
import org.lingoutil.autoapi.annotation.AutoApi;
import org.lingoutil.autoapi.config.ApiConfiguration;
import org.lingoutil.autoapi.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 生成 API 方法的前端 JavaScript 代码的类。
 * 该类会扫描指定的 Controller 类，根据其注解生成对应的 JavaScript API 方法文件。
 */
@Component
public class ApiMethodBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ApiMethodBuilder.class);

    @Resource
    private ApiConfiguration apiConfiguration;

    private static final String GET = "get";
    private static final String POST = "post";

    /**
     * 执行 API 方法生成过程。
     *
     * @param clazz 要处理的 Controller 类
     */
    public void execute(Class<?> clazz) {
        // 获取 API 文件的名称和路径
        int beginIndex = clazz.getName().lastIndexOf('.') + 1;
        String apiFileName = clazz.getName().substring(beginIndex).replace("Controller", "Api");
        String apiFolderPath = apiConfiguration.getOutputPath();
        File apiFolder = new File(apiFolderPath);

        // 如果目录不存在，则创建目录
        if (!apiFolder.exists()) {
            apiFolder.mkdirs();
        }

        OutputStream outputStream = null;
        OutputStreamWriter outputStreamWriter = null;
        BufferedWriter bufferedWriter = null;

        File apiFile = new File(apiFolder, apiFileName + ".js");
        try {
            // 创建 API 方法文件
            apiFile.createNewFile();
            // 初始化文件输出流
            outputStream = new FileOutputStream(apiFile);
            // 初始化输出流写入器，设置编码为 UTF-8
            outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            // 初始化缓冲写入器
            bufferedWriter = new BufferedWriter(outputStreamWriter);

            bufferedWriter.write("import request from '@/utils/request'");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            // 生成 API 方法
            generateApiMethod(clazz, bufferedWriter);
        }
        catch (Exception e) {
            logger.error("Failed to generate API method for JavaScript", e);
        }
        finally {
            // 关闭文件流
            FileUtils.closeQuietly(bufferedWriter);
            FileUtils.closeQuietly(outputStreamWriter);
            FileUtils.closeQuietly(outputStream);
        }
    }

    /**
     * 生成 API 方法的 JavaScript 代码。
     *
     * @param clazz          Controller 类
     * @param bufferedWriter 缓存字符流
     * @throws IOException 文件操作异常
     */
    private static void generateApiMethod(Class<?> clazz, BufferedWriter bufferedWriter) throws IOException {
        // 获取类上的 RequestMapping 注解
        RequestMapping requestMappingAnnotation = clazz.getAnnotation(RequestMapping.class);
        String[] requestPathArray = requestMappingAnnotation.value();
        String controllerPath = FileUtils.guaranteeStartWithSlash(requestPathArray[0]);

        // 获取类中的所有声明的方法
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            // 检查方法是否标注了 AutoApi 注解
            if (method.isAnnotationPresent(AutoApi.class)) {
                // 获取方法上的 RequestMapping 注解
                RequestMapping methodRequestMappingAnnotation = method.getAnnotation(RequestMapping.class);
                String methodPath = FileUtils.guaranteeStartWithSlash(methodRequestMappingAnnotation.value()[0]);

                // 获取 AutoApi 注解，并处理未指定的属性
                AutoApi autoApiAnnotation = method.getAnnotation(AutoApi.class);
                String path = autoApiAnnotation.path().isEmpty() ? controllerPath + methodPath : autoApiAnnotation.path();
                String httpMethod = autoApiAnnotation.method().isEmpty() ? autoSelectHttpMethod(method) : autoApiAnnotation.method();
                String description = autoApiAnnotation.description();

                // 写入方法的描述注释
                if (description != null && !description.isEmpty()) {
                    createMethodComment(description, bufferedWriter);
                }

                // 根据请求方法类型生成对应的 API 方法
                if (httpMethod.equals(GET)) {
                    bufferedWriter.write(String.format("export const %s = (query) => {", method.getName() + "Api"));
                    bufferedWriter.newLine();

                    // 写入请求参数的结构注释
                    createDataComment(method, bufferedWriter, GET);

                    bufferedWriter.write("\treturn request({");
                    bufferedWriter.newLine();
                    bufferedWriter.write(String.format("\t\turl: '%s',", path));
                    bufferedWriter.newLine();
                    bufferedWriter.write(String.format("\t\tmethod: '%s',", httpMethod));
                    bufferedWriter.newLine();
                    bufferedWriter.write("\t\tparams: query");
                    bufferedWriter.newLine();
                    bufferedWriter.write("\t})");
                    bufferedWriter.newLine();

                    bufferedWriter.write("}");
                    bufferedWriter.newLine();
                }
                else {
                    bufferedWriter.write(String.format("export const %s = (data) => {", method.getName() + "Api"));
                    bufferedWriter.newLine();

                    // 写入请求参数的结构注释
                    createDataComment(method, bufferedWriter, POST);

                    bufferedWriter.write("\treturn request({");
                    bufferedWriter.newLine();
                    bufferedWriter.write(String.format("\t\turl: '%s',", path));
                    bufferedWriter.newLine();
                    bufferedWriter.write(String.format("\t\tmethod: '%s',", httpMethod));
                    bufferedWriter.newLine();
                    bufferedWriter.write("\t\tdata");
                    bufferedWriter.newLine();
                    bufferedWriter.write("\t})");
                    bufferedWriter.newLine();

                    bufferedWriter.write("}");
                    bufferedWriter.newLine();
                }
                bufferedWriter.newLine();
            }
        }
    }

    /**
     * 生成方法的描述注释。
     *
     * @param comment        注释内容
     * @param bufferedWriter 缓存字符流
     * @throws IOException 文件操作异常
     */
    private static void createMethodComment(String comment, BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write("/**");
        bufferedWriter.newLine();
        bufferedWriter.write(String.format(" * %s", comment));
        bufferedWriter.newLine();
        bufferedWriter.write(" */");
        bufferedWriter.newLine();
    }

    /**
     * 根据方法参数判断请求方法类型。
     * 如果参数中包含自定义对象或 @RequestBody 注解，则返回 POST，否则返回 GET。
     *
     * @param method 方法
     * @return 请求方法类型（POST 或 GET）
     */
    private static String autoSelectHttpMethod(Method method) {
        return hasCustomObject(method) || hasRequestBodyAnnotation(method) ? POST : GET;
    }

    /**
     * 判断方法参数中是否存在 @RequestBody 注解。
     *
     * @param method 方法
     * @return 如果存在 @RequestBody 注解，返回 true；否则返回 false。
     */
    private static boolean hasRequestBodyAnnotation(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < parameterTypes.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType().equals(RequestBody.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断方法参数中是否包含自定义对象。
     *
     * @param method 方法
     * @return 如果参数中包含自定义对象，返回 true；否则返回 false。
     */
    private static boolean hasCustomObject(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        for (Class<?> parameterType : parameterTypes) {
            if (isCustomObject(parameterType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断给定的类是否为自定义对象。
     * 自定义对象定义为非基本数据类型、非包装类、非字符串、非日期类以及非数组类。
     *
     * @param clazz 类文件
     * @return 如果是自定义对象，返回 true；否则返回 false。
     */
    private static boolean isCustomObject(Class<?> clazz) {
        // 判断是否为基本数据类型（如 int, double, boolean 等）
        if (clazz.isPrimitive()) {
            return false;
        }

        // 判断是否为包装类（如 Integer, Double, Boolean 等）
        if (clazz.equals(Boolean.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Double.class)) {
            return false;
        }

        // 判断是否为 String 类型
        if (clazz.equals(String.class)) {
            return false;
        }

        // 判断是否为常见的日期类型（如 Date, LocalDate, LocalDateTime 等）
        if (clazz.equals(java.util.Date.class) ||
                clazz.equals(java.time.LocalDate.class) ||
                clazz.equals(java.time.LocalDateTime.class) ||
                clazz.equals(java.time.LocalTime.class) ||
                clazz.equals(java.time.ZonedDateTime.class)) {
            return false;
        }

        // 如果类是数组，也返回 false
        if (clazz.isArray()) {
            return false;
        }

        // 其他情况，认为是自定义对象
        return true;
    }

    /**
     * 生成请求参数的 JSON 格式的数据注释。
     * 根据方法的参数类型生成对应的 JSON 格式数据注释，支持 GET 和 POST 请求。
     *
     * @param method         方法
     * @param bufferedWriter 缓存字符流
     * @param httpMethod     HTTP 请求方法（GET 或 POST）
     * @throws IOException 文件操作异常
     */
    private static void createDataComment(Method method, BufferedWriter bufferedWriter, String httpMethod) throws IOException {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 如果没有参数，直接返回，不产生注释
        if (parameterTypes.length == 0) {
            return;
        }

        bufferedWriter.write("\t/*");
        bufferedWriter.newLine();

        // 根据 HTTP 请求方法写入不同的参数注释
        if (httpMethod.equals(GET)) {
            bufferedWriter.write("\t\tquery: {");
        }
        else {
            bufferedWriter.write("\t\tdata: {");
        }
        bufferedWriter.newLine();

        // 写入参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];

            boolean isRequestBody = false;
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation.annotationType().equals(RequestBody.class)) {
                    isRequestBody = true;
                    break;
                }
            }

            if (isRequestBody || isCustomObject(parameterType)) {
                // 处理 @RequestBody 参数
                writeObjectFields(bufferedWriter, parameterType, "\t\t\t");
                bufferedWriter.write("\t\t}");
                bufferedWriter.newLine();
            }
            else {
                // 处理普通参数
                String parameterName = method.getParameters()[i].getName(); // 使用实际的参数名称
                bufferedWriter.write(String.format("\t\t\t%s: %s,", parameterName, parameterType.getSimpleName()));
                bufferedWriter.newLine();
                if (i == parameterTypes.length - 1) {
                    bufferedWriter.write("\t\t}");
                    bufferedWriter.newLine();
                }
            }
        }

        bufferedWriter.write("\t*/");
        bufferedWriter.newLine();
    }

    /**
     * 递归写入对象字段的 JSON 格式数据注释。
     *
     * @param bufferedWriter 缓存字符流
     * @param clazz          对象类
     * @param indent         缩进字符
     * @throws IOException 文件操作异常
     */
    private static void writeObjectFields(BufferedWriter bufferedWriter, Class<?> clazz, String indent) throws IOException {
        // 获取所有的字段
        java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

        for (java.lang.reflect.Field field : fields) {
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();

            // 判断字段类型
            if (isCustomObject(fieldType)) {
                // 如果是自定义对象则递归调用解析写入
                bufferedWriter.write(String.format("%s%s: {", indent, fieldName));
                bufferedWriter.newLine();
                writeObjectFields(bufferedWriter, fieldType, indent + "\t");
                bufferedWriter.write(indent + "},");
            }
            else {
                // 如果是非自定义对象，则写入
                bufferedWriter.write(String.format("%s%s: %s,", indent, fieldName, fieldType.getSimpleName()));
            }
            bufferedWriter.newLine();
        }
    }
}
