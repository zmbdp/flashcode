package com.zmbdp.portal.service.flash.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Selenium 工具类<p>
 * 提供网页截图、图片压缩等功能
 *
 * @author 稚名不带撇
 */
@Slf4j
public class SeleniumUtil {

    /**
     * 截图保存目录
     */
    private static final String SCREENSHOT_DIR = "/user-app-image";

    /**
     * 压缩图片保存目录
     */
    private static final String COMPRESSED_DIR = "/user-app-image/compresse";

    /**
     * 截图文件后缀
     */
    private static final String SCREENSHOT_SUFFIX = ".jpg";

    /**
     * 压缩文件后缀
     */
    private static final String COMPRESSED_SUFFIX = "_compressed.jpg";

    /**
     * 默认页面加载超时时间（秒）
     */
    private static final int DEFAULT_PAGE_LOAD_TIMEOUT = 30;

    /**
     * 默认压缩比例
     */
    public static final float DEFAULT_COMPRESSION_QUALITY = 0.3f;

    /**
     * 默认窗口大小（宽x高）
     */
    private static final String DEFAULT_WINDOW_SIZE = "1920,1080";

    /**
     * 一站式截图并压缩（自定义压缩比和窗口大小）
     *
     * @param url        目标网页地址
     * @param quality    压缩质量（0.0-1.0）
     * @return 压缩后的图片文件路径，如果失败则返回 null
     */
    public static String screenshot(String url, float quality) {
        log.info("---开始截图---");
        String screenshotPath = captureScreenshot(url, generateFileName(), DEFAULT_PAGE_LOAD_TIMEOUT, DEFAULT_WINDOW_SIZE);
        if (screenshotPath == null) {
            log.error("截图并压缩失败：截图阶段失败");
            return null;
        }

        String compressedPath = compressImage(screenshotPath, quality);
        if (compressedPath == null) {
            log.error("截图并压缩失败：压缩阶段失败");
            return null;
        }

        return compressedPath;
    }

    /**
     * 访问指定 URL 并对首页进行截图（完整参数，包含窗口大小）
     *
     * @param url            目标网页地址
     * @param fileName       截图文件名（不含后缀）
     * @param timeoutSeconds 页面加载超时时间（秒）
     * @param windowSize     窗口大小，格式：宽,高（如：1920,1080）
     * @return 截图文件的完整路径，如果失败则返回 null
     */
    private static String captureScreenshot(String url, String fileName, int timeoutSeconds, String windowSize) {
        // 参数校验
        if (StrUtil.isBlank(url)) {
            log.error("截图失败：URL 不能为空");
            return null;
        }
        if (StrUtil.isBlank(fileName)) {
            fileName = generateFileName();
        }
        if (StrUtil.isBlank(windowSize)) {
            windowSize = DEFAULT_WINDOW_SIZE;
        }

        WebDriver driver = null;
        try {
            // 确保目录存在
            String userDir = System.getProperty("user.dir");
            String screenshotBasePath = Paths.get(userDir, SCREENSHOT_DIR).toAbsolutePath().toString();
            FileUtil.mkdir(screenshotBasePath);

            // 初始化 WebDriver
            driver = createWebDriver(windowSize);

            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));

            // 访问目标 URL
            log.info("开始访问页面：{}", url);
            driver.get(url);

            // 等待页面完全加载
            waitForPageLoad(driver, timeoutSeconds);

            // 执行截图
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            // 构建目标文件路径
            String targetPath = screenshotBasePath + File.separator + fileName + SCREENSHOT_SUFFIX;
            File targetFile = new File(targetPath);

            // 复制文件到目标位置
            FileUtil.copy(screenshotFile, targetFile, true);

            log.info("截图成功，保存路径：{}", targetPath);
            return targetPath;

        } catch (TimeoutException e) {
            log.error("截图失败：页面加载超时 - {}", url, e);
            return null;
        } catch (Exception e) {
            log.error("截图失败：{}", url, e);
            return null;
        } finally {
            // 关闭浏览器，释放资源
            quitWebDriver(driver);
        }
    }

    /**
     * 压缩图片文件（自定义压缩比）
     *
     * @param imagePath 原图片文件路径
     * @param quality   压缩质量（0.0-1.0），值越大质量越高，建议 0.5-0.9
     * @return 压缩后的图片文件路径，如果失败则返回 null
     */
    public static String compressImage(String imagePath, float quality) {
        // 参数校验
        if (StrUtil.isBlank(imagePath)) {
            log.error("压缩失败：图片路径不能为空");
            return null;
        }

        File originalFile = new File(imagePath);
        if (!originalFile.exists() || !originalFile.isFile()) {
            log.error("压缩失败：图片文件不存在 - {}", imagePath);
            return null;
        }

        if (quality <= 0 || quality > 1) {
            log.warn("压缩质量参数不合法（{}），使用默认值：{}", quality, DEFAULT_COMPRESSION_QUALITY);
            quality = DEFAULT_COMPRESSION_QUALITY;
        }

        try {
            // 确保压缩目录存在
            String userDir = System.getProperty("user.dir");
            String compressedBasePath = Paths.get(userDir, COMPRESSED_DIR).toAbsolutePath().toString();
            FileUtil.mkdir(compressedBasePath);

            // 构建压缩后的文件名和路径
            String originalFileName = FileUtil.mainName(originalFile);
            String compressedFileName = originalFileName + COMPRESSED_SUFFIX;
            String compressedPath = compressedBasePath + File.separator + compressedFileName;
            File compressedFile = new File(compressedPath);

            // 读取原图片
            BufferedImage image = ImageIO.read(originalFile);
            if (image == null) {
                log.error("压缩失败：无法读取图片文件 - {}", imagePath);
                return null;
            }

            // 使用 Hutool 压缩图片
            log.info("开始压缩图片：{}, 压缩质量：{}", imagePath, quality);
            ImgUtil.scale(
                    image,
                    compressedFile,
                    quality
            );

            // 验证压缩后的文件是否存在
            if (!compressedFile.exists()) {
                log.error("压缩失败：压缩文件未生成");
                return null;
            }

            // 删除原图片
            boolean deleted = FileUtil.del(originalFile);
            if (deleted) {
                log.info("原图片已删除：{}", imagePath);
            } else {
                log.warn("原图片删除失败：{}", imagePath);
            }

            long originalSize = originalFile.length();
            long compressedSize = compressedFile.length();
            double compressionRatio = (1 - (double) compressedSize / originalSize) * 100;

            return compressedPath;

        } catch (IOException e) {
            log.error("压缩图片时发生 IO 异常：{}", imagePath, e);
            return null;
        } catch (Exception e) {
            log.error("压缩图片失败：{}", imagePath, e);
            return null;
        }
    }
    /**
     * 创建 WebDriver 实例
     *
     * @param windowSize 窗口大小，格式：宽,高（如：1920,1080）
     * @return WebDriver 实例
     */
    private static WebDriver createWebDriver(String windowSize) {
        ChromeOptions options = new ChromeOptions();
        // 无头模式，不打开浏览器界面
        options.addArguments("--headless");

        // 禁用 GPU 加速（Linux 环境推荐）
        options.addArguments("--disable-gpu");

        // 禁用沙箱模式（Docker 环境必需）
        options.addArguments("--no-sandbox");

        // 禁用 /dev/shm 使用（防止资源限制）
        options.addArguments("--disable-dev-shm-usage");

        // 设置窗口大小
        if (StrUtil.isNotBlank(windowSize)) {
            options.addArguments("--window-size=" + windowSize);
            log.debug("设置窗口大小：{}", windowSize);
        } else {
            options.addArguments("--window-size=" + DEFAULT_WINDOW_SIZE);
            log.debug("使用默认窗口大小：{}", DEFAULT_WINDOW_SIZE);
        }

        // 禁用图片加载以加快速度（可选，如果需要截图建议注释掉）
        // options.addArguments("--blink-settings=imagesEnabled=false");

        // 忽略证书错误
        options.addArguments("--ignore-certificate-errors");

        log.debug("创建 Chrome WebDriver");
        return new ChromeDriver(options);
    }

    /**
     * 等待页面完全加载
     *
     * @param driver         WebDriver 实例
     * @param timeoutSeconds 超时时间（秒）
     */
    private static void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));

        // 等待页面加载状态为 complete
        ExpectedCondition<Boolean> pageLoadCondition = webDriver -> {
            JavascriptExecutor js = (JavascriptExecutor) webDriver;
            String readyState = js.executeScript("return document.readyState").toString();
            log.debug("页面加载状态：{}", readyState);
            return "complete".equals(readyState);
        };

        wait.until(pageLoadCondition);

        // 额外等待一小段时间，确保动态内容加载完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("等待页面加载时被中断", e);
        }

        log.info("页面加载完成");
    }

    /**
     * 安全关闭 WebDriver
     *
     * @param driver WebDriver 实例
     */
    private static void quitWebDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                log.debug("WebDriver 已关闭");
            } catch (Exception e) {
                log.warn("关闭 WebDriver 时发生异常", e);
            }
        }
    }

    /**
     * 生成基于时间戳的文件名
     *
     * @return 文件名（不含后缀）
     */
    private static String generateFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
        return "screenshot_" + LocalDateTime.now().format(formatter);
    }
}

