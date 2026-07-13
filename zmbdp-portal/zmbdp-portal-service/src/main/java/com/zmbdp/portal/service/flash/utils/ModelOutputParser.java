package com.zmbdp.portal.service.flash.utils;

import com.zmbdp.portal.service.flash.enums.AppType;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型输出解析器
 *
 * @author 稚名不带撇
 */
public class ModelOutputParser {

    /**
     * 解析模型输出，返回解析结果
     *
     * @param output 模型输出
     * @return 解析结果
     */
    public static ParsedResult parse(String output) {
        ParsedResult result = new ParsedResult();
        if (output == null) {
            return result;
        }

        // 移除 <think>...</think> 标签及其内容
        output = output.replaceAll("(?s)<think>.*?</think>", "");

        String[] lines = output.split("\r?\n");

        // Parse FILE blocks first
        String currentPath = null;
        StringBuilder buf = null;
        boolean inFence = false;
        for (String line : lines) {
            String t = line;
            if (t.startsWith("FILE:")) {
                // flush previous
                if (currentPath != null && buf != null) {
                    result.files.put(currentPath, buf.toString());
                }
                currentPath = t.substring("FILE:".length()).trim();
                buf = new StringBuilder();
                inFence = false;
                continue;
            }
            if (t.startsWith("```")) {
                // toggle fence, but do not include fence lines
                inFence = !inFence;
                continue;
            }
            if (inFence && buf != null) {
                buf.append(line).append("\n");
            }
        }
        // flush last
        if (currentPath != null && buf != null) {
            result.files.put(currentPath, buf.toString());
        }

        // Parse APP_TYPE based on file analysis
        result.appType = determineAppType(result.files);

        return result;
    }

//        - HTML：整个应用只有一个 HTML 页面，不依赖任何后端服务。     仅包含一个文件，并且文件的后缀是 .html
//        - VUE：基于 Vue3 构建的纯前端应用，不依赖后端服务。       只包含 .vue  不包含 .java
//        - VUE_SPRING：完整的前后端分离应用，前端基于 Vue3 架构，后端采用 SpringBoot 框架。  既包含 .vue 文件又包含 .java 文件
    private static String determineAppType(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }

        // 规则 1: 如果仅生成了一个文件并且文件后缀为 .html，则应用类型为 HTML
        if (files.size() == 1) {
            String singleFile = files.keySet().iterator().next();
            if (singleFile.toLowerCase().endsWith(".html")) {
                return AppType.HTML.getType();
            }
        }

        // 规则 2: 如果生成的文件同时包含 .java 文件和 .vue 文件，则应用类型为 VUE_SPRING
        boolean hasJavaFile = files.keySet().stream()
                .anyMatch(path -> path.toLowerCase().endsWith(".java"));
        boolean hasVueFile = files.keySet().stream()
                .anyMatch(path -> path.toLowerCase().endsWith(".vue"));
        if (hasJavaFile && hasVueFile) {
            return AppType.VUE_SPRING.getType();
        }

        // 规则 3: 如果既不是 HTML 类型也不是 VUE_SPRING 类型，并且生成的文件中包含.vue文件，则应用类型为VUE
        if (hasVueFile) {
            return AppType.VUE.getType();
        }

        return "error";
    }

    @Data
    public static class ParsedResult {

        /**
         * 应用类型
         */
        private String appType;

        /**
         * 文件内容
         */
        private Map<String, String> files = new LinkedHashMap<>();
    }
}