# Keepo

### Importing the project

Intellij has native support for `sbt` projects. Please, see details [here](https://blog.jetbrains.com/scala/2013/11/18/built-in-sbt-support-in-intellij-idea-13/).

### Running the project locally

You may launch the app from your ide for debugging or running. Please, see details [here](https://www.playframework.com/documentation/2.5.x/IDE).

Or instead, run with `sbt run`.

### Requirements

We are working with Docker containers running instances of [Morbid](https://github.com/leandrocruz/morbid) and Hashicorp's [Vault](https://hub.docker.com/_/vault).

### Environment

To set up the environment, run the commands below:
```bash
$ sudo mkdir -p /opt/keepo/vault/shared/conf
$ sudo chown -R $USER. /opt/keepo/
$ echo 'include "application.conf"' > /opt/keepo/vault/shared/conf/local.conf
```

### Running

Run the container with:
```bash
$ docker run --name keepo -p 9005:9005 -d -v /opt/keepo/vault/shared:/opt/keepo/service/shared oystrcombr/keepo:latest
```

### Making requests
The service documentation can be found [here](https://documenter.getpostman.com/view/1591099/SzYgQErT).
