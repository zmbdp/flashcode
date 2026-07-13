package com.zmbdp.portal.service.flash.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 生成代码工具类
 */
@Slf4j
public class GeneratedAppWriter {

    /**
     * 确保 usercode 目录存在
     */
    public static Path ensureUserCodeDir() throws IOException {
        String userDir = System.getProperty("user.dir");
        Path base = Paths.get(userDir, "user-code").toAbsolutePath();
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        return base;
    }

    /**
     * 将文件写入 usercode 目录。
     *
     * @param id    应用 ID
     * @param files 文件列表
     * @return 应用目录
     */
    public static Path writeFiles(Long id, Map<String, String> files) throws IOException {
        return writeFiles(id.toString(), files, false);
    }

    /**
     * 将文件写入 usercode 目录。
     *
     * @param id         应用 ID
     * @param files      文件列表
     * @param cleanFirst 是否先清理已存在的目录
     * @return 应用目录
     */
    public static Path writeFiles(String id, Map<String, String> files, boolean cleanFirst) throws IOException {
        Path base = ensureUserCodeDir();
        Path appDir = base.resolve(id);
        log.info("Generated app: {}", appDir);

        // 如果需要清理且目录存在，先删除旧文件
        if (cleanFirst && Files.exists(appDir)) {
            log.info("清理旧代码目录: {}", appDir);
            deleteDirectory(appDir);
        }

        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
        }

        for (Map.Entry<String, String> e : files.entrySet()) {
            String rel = e.getKey();
            Path target = appDir.resolve(rel).normalize();
            if (!target.startsWith(appDir)) {
                // prevent path traversal
                log.warn("跳过不安全的路径: {}", rel);
                continue;
            }
            if (target.getParent() != null && !Files.exists(target.getParent())) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, e.getValue(), StandardCharsets.UTF_8);
        }
        return appDir;
    }

    /**
     * 递归删除目录
     */
    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> b.compareTo(a)) // 逆序，先删除文件再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
        }
    }
}
