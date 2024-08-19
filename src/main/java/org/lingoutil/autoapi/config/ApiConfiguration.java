package org.lingoutil.autoapi.config;

import org.lingoutil.autoapi.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiConfiguration {

    @Value("${lingo-util.auto-api.output-path:./}")
    private String outputPath;

    @Value("${lingoutil.auto-api.enable:true}")
    private Boolean enable;

    public String getOutputPath() {
        return FileUtils.guaranteeEndWithSlash(outputPath);
    }

    public Boolean getEnable() {
        if (enable == null) {
            return true;
        }
        return enable;
    }
}
