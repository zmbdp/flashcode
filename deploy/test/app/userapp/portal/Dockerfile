FROM crpi-otuh3k08p4yxr3tv.cn-shanghai.personal.cr.aliyuncs.com/zmbdp-infra/eclipse-temurin:21-jre

LABEL maintainer="JavaFH@163.com"

ENV TZ=Asia/Shanghai
ENV MAVEN_VERSION=3.9.14
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ---------- 1. 一次性安装所有系统依赖 ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        wget && \
    rm -rf /var/lib/apt/lists/*

# ---------- 2. 安装 Maven（稳定缓存层） ----------
RUN wget -q https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    -O /tmp/maven.tar.gz && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    rm -f /tmp/maven.tar.gz

# ---------- 3. 安装 Node.js（稳定缓存层） ----------
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get update && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

CMD ["bash"]