FROM oystrcombr/oystr-base-java:latest
MAINTAINER Rafael Silvério - rafael.silverio.it@gmail.com - rafael.silverio@oystr.com.br

LABEL oystr.container.type=oystr-service

ADD bin  /opt/keepo/service/bin
ADD conf /opt/keepo/service/conf
ADD lib  /opt/keepo/service/lib

RUN mkdir -p /etc/service/keepo-service
ADD run      /etc/service/keepo-service/run
