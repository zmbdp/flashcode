package com.zmbdp.portal.service.flash.utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * 应用构建工具类
 *
 * @author 稚名不带撇
 */
@Slf4j
public class AppBuildUtil {

    /**
     * 构建 Vue 项目
     *
     * @param appId             应用 ID
     * @param nodeProjectDir    Node 项目目录
     * @param previewDeployPath 预览部署目录
     */
    public static void buildVuePro(Long appId, Path nodeProjectDir, String previewDeployPath) {
        try {
            runProcess(List.of("npm", "install"), nodeProjectDir);
            runProcess(List.of("npm", "run", "build"), nodeProjectDir);
            Path distDir = nodeProjectDir.resolve("dist");
            Path targetDistDir = FileUtil.ensureAppDir(appId, previewDeployPath).resolve("dist");
            FileUtil.copyDirectory(distDir, targetDistDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建 Spring Boot 项目
     *
     * @param appId             应用 ID
     * @param springRootDir     Spring 项目根目录
     * @param dockerClient      Docker 客户端
     * @param previewDeployPath 预览部署目录
     * @param containerName     容器名称
     */
    public static void buildSpringBoot(Long appId, Path springRootDir, DockerClient dockerClient, String previewDeployPath, String containerName) {
        try {
            runProcess(List.of("mvn", "clean", "package", "-DskipTests"), springRootDir);
            Path targetDir = springRootDir.resolve("target");
            Path previewDir = FileUtil.ensureAppDir(appId, previewDeployPath);
            if (Files.exists(targetDir)) {
                try (Stream<Path> s = Files.list(targetDir)) {
                    Path jar = s
                            .filter(p -> p.getFileName().toString().endsWith(".jar"))
                            .findFirst()
                            .orElse(null);
                    if (jar != null) {
                        Files.copy(jar, previewDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                        log.info("已将后端 jar 复制到预览目录: {}", previewDir.resolve(jar.getFileName()));

                        // 在容器中执行 jar 包
                        try {
                            executeJarInContainer(appId, jar.getFileName().toString(), dockerClient, previewDeployPath, containerName);
                        } catch (Exception e) {
                            log.error("在容器中执行 jar 包失败，appId={}, jar={}, 错误: {}", appId, jar.getFileName(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("未在 target 目录找到 jar 文件，appId={}", appId);
                    }
                }
            } else {
                log.warn("后端构建目录不存在: {}", targetDir);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 在容器中执行 jar 包
     *
     * @param appId             应用 ID
     * @param jarFileName       jar 文件名
     * @param dockerClient      Docker 客户端
     * @param previewDeployPath 预览部署目录
     * @param containerName     容器名称
     */
    private static void executeJarInContainer(Long appId, String jarFileName, DockerClient dockerClient, String previewDeployPath, String containerName) {
        try {
            // 查找容器
            Container container = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .filter(c -> Arrays.asList(c.getNames()).contains("/" + containerName))
                    .findFirst()
                    .orElse(null);

            if (container == null) {
                log.warn("未找到容器: {}", containerName);
                return;
            }

            String containerId = container.getId();

            int port = generatePort(appId, previewDeployPath);

            String jarPathInContainer = "/workspace/user-preview" + "/" + appId + "/" + jarFileName;
            if ("user-deploy".equals(previewDeployPath)) {
                jarPathInContainer = "/workspace/user-deploy" + "/" + jarFileName;
            }

            // 使用 nohup 在后台执行 jar 包，指定端口号
            String command = "nohup java -jar " + jarPathInContainer + " --server.port=" + port + " > /dev/null 2>&1 &";

            log.info("在容器 {} 中执行命令: {}", containerName, command);

            String execId = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd("bash", "-c", command)
                    .exec()
                    .getId();

            dockerClient.execStartCmd(execId).start();

            log.info("已在容器 {} 中启动 jar 包: {}，端口: {}", containerName, jarPathInContainer, port);

            log.info("--- previewDeployPath: {}" , previewDeployPath);
            // 动态更新 Nginx 配置
            if ("user-preview".equals(previewDeployPath)) {
                updateNginxConfig(containerId, appId, port, dockerClient);
            }

        } catch (Exception e) {
            log.error("在容器中执行 jar 包失败，容器名={}, appId={}, jar={}, 错误: {}",
                    containerName, appId, jarFileName, e.getMessage(), e);
            throw new RuntimeException("在容器中执行 jar 包失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成端口号
     *
     * @param appId             应用 ID
     * @param previewDeployPath 预览部署目录
     * @return 端口号
     */
    private static int generatePort(Long appId, String previewDeployPath) {
        // 使用 appId 生成固定端口，范围：8001-9999
        // 8001 + (appId % 1999) 可以生成 8001-9999 范围内的端口
        if ("user-deploy".equals(previewDeployPath)) {
            return 8080; // 部署固定使用 8080 端口
        }
        int port = 8001 + (int) (appId % 1999);
        log.info("为 appId {} 分配端口: {}", appId, port);
        return port;
    }

    /**
     * 更新 Nginx 配置
     *
     * @param containerId 容器 ID
     * @param appId       应用 ID
     * @param port        端口
     */
    private static void updateNginxConfig(String containerId, Long appId, int port, DockerClient dockerClient) {
        log.info("更新 nginx 配置，appId={}, port={}", appId, port);
        String scriptPath = "/workspace/scripts/update_nginx_location.sh";
        String updateCommand = scriptPath + " " + appId + " " + port;
        String reloadCommand = "nginx -s reload";

        try {
            execInContainer(containerId, updateCommand, "更新 nginx 配置", dockerClient);
            execInContainer(containerId, reloadCommand, "重载 nginx", dockerClient);
            log.info("容器 {} 的 nginx 配置已更新并重载，appId={}, port={}", containerId, appId, port);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("更新 nginx 配置时线程被中断，appId={}, port={}", appId, port, ie);
            throw new RuntimeException("更新 nginx 配置过程中线程被中断", ie);
        } catch (Exception e) {
            log.error("更新 nginx 配置失败，appId={}, port={}, 错误: {}", appId, port, e.getMessage(), e);
            throw new RuntimeException("更新 nginx 配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在指定容器中执行命令并校验退出码
     *
     * @param containerId 容器 ID
     * @param command     需要执行的命令
     * @param actionDesc  日志描述
     */
    private static void execInContainer(String containerId, String command, String actionDesc, DockerClient dockerClient)
            throws IOException, InterruptedException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        String execId = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("bash", "-c", command)
                .exec()
                .getId();

        dockerClient.execStartCmd(execId)
                .exec(new ExecStartResultCallback(stdout, stderr))
                .awaitCompletion();

        InspectExecResponse inspect = dockerClient.inspectExecCmd(execId).exec();
        Long exitCode = inspect.getExitCodeLong();
        if (exitCode == null || exitCode != 0) {
            String errorOutput = stderr.toString(StandardCharsets.UTF_8);
            throw new IOException(actionDesc + "失败，退出码=" + exitCode + "，错误输出: " + errorOutput);
        }

        if (stdout.size() > 0) {
            log.info("{} 成功，输出: {}", actionDesc, stdout.toString(StandardCharsets.UTF_8).trim());
        } else {
            log.info("{} 成功", actionDesc);
        }
    }

    /**
     * 运行命令
     */
    public static void runProcess(List<String> command, Path cwd) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);

        // 捕获输出以便在错误时提供详细信息
        StringBuilder outputBuilder = new StringBuilder();
        try {
            Process p = pb.start();

            // 读取进程输出
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuilder.append(line).append("\n");
                    System.out.println(line); // 保持实时输出
                }
            }

            int code = p.waitFor();
            if (code != 0) {
                String errorMsg = String.format(
                        "命令执行失败\n命令: %s\n工作目录: %s\n退出码: %d\n输出:\n%s",
                        String.join(" ", command),
                        cwd,
                        code,
                        truncateOutput(outputBuilder.toString(), 2000)
                );
                throw new IOException(errorMsg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = String.format(
                    "命令执行被中断\n命令: %s\n工作目录: %s\n已捕获输出:\n%s",
                    String.join(" ", command),
                    cwd,
                    truncateOutput(outputBuilder.toString(), 1000)
            );
            throw new IOException(errorMsg, e);
        }
    }

    /**
     * 截断输出以避免过长的错误消息
     */
    private static String truncateOutput(String output, int maxLength) {
        if (output.length() <= maxLength) {
            return output;
        }

        // 保留开头和结尾的部分内容
        int halfLength = maxLength / 2;
        return output.substring(0, halfLength) +
                "\n... (省略 " + (output.length() - maxLength) + " 字符) ...\n" +
                output.substring(output.length() - halfLength);
    }
}