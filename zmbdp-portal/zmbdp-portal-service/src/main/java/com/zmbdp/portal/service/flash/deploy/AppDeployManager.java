package com.zmbdp.portal.service.flash.deploy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AppDeployManager {

    /**
     * 动态端口起始值
     */
    private final AtomicInteger portAllocator = new AtomicInteger(9000);

    /**
     * appId -> port
     */
    private final ConcurrentHashMap<Long, Integer> appPortMap = new ConcurrentHashMap<>();

    /**
     * 分配端口
     */
    public int allocatePort(Long appId) {

        Integer existingPort = appPortMap.get(appId);

        if (existingPort != null) {
            return existingPort;
        }

        int port = portAllocator.getAndIncrement();

        while (!isPortAvailable(port)) {
            port = portAllocator.getAndIncrement();
        }

        appPortMap.put(appId, port);

        log.info("应用 [{}] 分配端口 [{}]", appId, port);

        return port;
    }

    /**
     * 检测端口是否可用
     */
    private boolean isPortAvailable(int port) {

        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}