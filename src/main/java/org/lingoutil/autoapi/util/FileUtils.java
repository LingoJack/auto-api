package org.lingoutil.autoapi.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * 提供文件操作相关的实用工具方法。
 */
public class FileUtils {

    // 常量用于指示从文件名中获取的信息类型
    public static final int FILE_SUFFIX = 0;   // 仅获取文件后缀
    public static final int FILE_NAME_WITHOUT_SUFFIX = 1; // 获取不带后缀的文件名

    /**
     * 辅助方法用于安静关闭资源，避免在finally块中抛出异常覆盖原始异常。
     *
     * @param closable 可关闭的资源对象
     */
    public static void closeQuietly(Closeable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        }
        catch (IOException ignore) {
            // 忽略关闭时的异常
        }
    }

    /**
     * 确保一个路径名以/结尾
     *
     * @param filePath
     * @return
     */
    public static String guaranteeEndWithSlash(String filePath) {
        if (filePath.endsWith("/")) {
            return filePath;
        }
        else {
            return filePath + "/";
        }
    }

    /**
     * 确保一个路径名以/开头
     *
     * @param filePath
     * @return
     */
    public static String guaranteeStartWithSlash(String filePath) {
        if (filePath.startsWith("/")) {
            return filePath;
        }
        else {
            return "/" + filePath;
        }
    }
}
