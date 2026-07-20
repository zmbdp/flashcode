package com.zmbdp.portal.service.flash.utils;

import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
public final class FileUtil {

    private static final ConcurrentHashMap<Long, Process> RUNNING_JARS = new ConcurrentHashMap<>();

    private FileUtil() {}

    /**
     * 确保 preview 基础目录存在
     */
    public static Path ensureBaseDir(String path) throws IOException {
        String userDir = System.getProperty("user.dir");
        Path base = Paths.get(userDir, path).toAbsolutePath();
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        return base;
    }

    /**
     * 确保 preview 应用的目录存在
     */
    public static Path ensureAppDir(Long appId, String path) throws IOException {
        Path base = ensureBaseDir(path);
        Path appDir = base.resolve(appId.toString());
        if (!Files.exists(appDir)) {
            Files.createDirectories(appDir);
        }
        return appDir;
    }

    /**
     * 清理指定应用的预览目录（保留目录本身）。
     * <p>
     * 用于同一 appId 重复生成时，清理旧的构建产物（dist、jar 等），
     * 避免旧文件残留干扰新一次预览。
     *
     * @param appId 应用 ID
     * @param path  预览基础目录名（如 user-preview / user-deploy）
     */
    public static void cleanAppDir(Long appId, String path) throws IOException {
        Path base = ensureBaseDir(path);
        Path appDir = base.resolve(appId.toString());
        if (!Files.exists(appDir)) {
            return;
        }
        log.info("清理预览目录: {}", appDir);
        try (var stream = Files.walk(appDir)) {
            stream.sorted((a, b) -> b.compareTo(a)) // 逆序，先删文件再删子目录
                    .filter(p -> !p.equals(appDir)) // 保留应用目录本身
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", p, e);
                        }
                    });
        }
    }

    /**
     * 复制目录
     */
    public static void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("源目录不存在: " + source);
        }
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        try (var stream = Files.walk(source)) {
            for (Path s : (Iterable<Path>) stream::iterator) {
                Path relative = source.relativize(s);
                Path dest = target.resolve(relative);
                if (Files.isDirectory(s)) {
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest);
                    }
                } else {
                    if (dest.getParent() != null && !Files.exists(dest.getParent())) {
                        Files.createDirectories(dest.getParent());
                    }
                    Files.copy(s, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static String saveFile(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("文件为空");
        }
        String userDir = System.getProperty("user.dir");
        Path basePath = Paths.get(userDir, "/tmp").toAbsolutePath();
        try {
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
            }
            String suffix = getSuffix(file.getOriginalFilename());
            fileName = fileName + suffix;
            Path filePath = basePath.resolve(fileName);
            InputStream is = file.getInputStream();
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    private static String getSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}