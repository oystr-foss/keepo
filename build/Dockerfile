FROM oystr-base-java:v1.0
MAINTAINER Rafael Silvério - rafael.silverio.it@gmail.com - rafael.silverio@oystr.com.br

LABEL oystr.container.type=oystr-service

ADD bin  /opt/oystr/service/bin
ADD conf /opt/oystr/service/conf
ADD lib  /opt/oystr/service/lib

RUN mkdir -p /etc/service/oystr-service
ADD run      /etc/service/oystr-service/run
