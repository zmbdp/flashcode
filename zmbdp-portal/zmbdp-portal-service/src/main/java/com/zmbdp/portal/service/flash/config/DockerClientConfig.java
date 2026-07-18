package com.zmbdp.portal.service.flash.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import com.zmbdp.common.domain.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * docker client 实例
 *
 * @author 稚名不带撇
 */
@Slf4j
@Configuration
public class DockerClientConfig {

    /**
     * docker 的主机地址
     */
    @Value("${docker.host}")
    private String dockerHost;

    /**
     * docker 的证书路径
     */
    @Value("${docker.cert-path}")
    private String dockerCertPath;

    /**
     * 创建 docker client 实例
     *
     * @return docker client
     */
    @Bean
    public DockerClient dockerClient() {
        log.info("Docker host: {}", dockerHost);
        if (StringUtils.isBlank(dockerHost)) {
            throw new ServiceException("Docker host is not set");
        }

        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(true)
                .withDockerCertPath(dockerCertPath)
                .build();
        return DockerClientBuilder
                .getInstance(clientConfig)
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                .build();
    }
}
