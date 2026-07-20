FROM crpi-otuh3k08p4yxr3tv.cn-shanghai.personal.cr.aliyuncs.com/zmbdp-infra/eclipse-temurin:21-jdk

LABEL maintainer="JavaFH@163.com"

ENV TZ=Asia/Shanghai
ENV MAVEN_VERSION=3.9.14
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ---------- 1. 安装系统依赖 ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl wget git ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# ---------- 2. 安装 Maven ----------
# 清华镜像站已停服，改用华为云镜像站（国内速度极快）
RUN wget -q https://mirrors.huaweicloud.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    -O /tmp/maven.tar.gz && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    rm -f /tmp/maven.tar.gz

# ---------- 3. 安装 Node.js ----------
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

# ---------- 4. 应用启动 ----------
WORKDIR /workspace

# 镜像源配置（阿里云 Maven + 淘宝 npm），构建时 COPY 进镜像，无需服务器手动放文件
COPY maven-settings.xml /root/.m2/settings.xml
COPY npmrc /root/.npmrc

COPY target/*.jar /workspace/app.jar
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]