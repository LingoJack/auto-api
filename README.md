`@author`：LingoJack

`email`：3065225677@qq.com

一个可以根据Controller接口来自动生成前端js请求api代码包的工具。

启动Application后自动在指定目录下生成js请求api代码

你可以在配置文件中指定输出的路径（默认为当前工作目录，可以加或不加最后的斜杠）以及是否启用该组件（默认启用）

`application.yml`

~~~yml
lingo-util:
  auto-api:
    output-path: ./
    enable: true
~~~



使用`@GenerateApi`注解来标注需要生成接口api的Controller类，enable属性提供类级别粒度的启用禁用控制；

使用`@AutoApi`注解标注Controller下需要生成接口api的方法，可以自定义路径和注释以及请求方法，如未指定，则会自动根据接口路径以及接口方法声明进行api的生成



maven依赖：（目前还未上传至远程仓库 2024.8.19）

~~~xml
<dependency>
    <groupId>org.lingoutil.autoapi</groupId>
    <artifactId>auto-api</artifactId>
    <version>1.0.0</version>
</dependency>
~~~



使用前需要配置好配置类：

`ApiGeneratorConfig.java`

~~~java
import org.lingoutil.autoapi.generator.ApiInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.lingoutil.autoapi")
public class AppConfig {
    @Bean
    public ApiInitializer apiInitializer() {
        return new ApiInitializer();
    }
}
~~~