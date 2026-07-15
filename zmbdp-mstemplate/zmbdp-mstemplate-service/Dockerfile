FROM crpi-otuh3k08p4yxr3tv.cn-shanghai.personal.cr.aliyuncs.com/zmbdp-infra/eclipse-temurin:21-jre
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /workspace
MAINTAINER JavaFH@163.com
COPY target/*.jar /workspace/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar