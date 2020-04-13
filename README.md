# Importing the Project
Intellij has native support for `sbt` projects. Please, see details [here](https://blog.jetbrains.com/scala/2013/11/18/built-in-sbt-support-in-intellij-idea-13/).

# Running the Project Locally
You may launch the app from your ide for debugging or running. Please, see details [here](https://www.playframework.com/documentation/2.5.x/IDE).

# Running Cassandra Locally
For local development you may run cassandra locally from docker with the following command:
`docker run --name cassandra-akka -p 7000:7000 -p 7199:7199 -p 9042:9042 -p 9160:9160 -d cassandra`.

The keyspaces and tables required by the application are created automatically for you if the application is started and the `oystr_execution_service` and `oystr_execution_service_snapshot` keyspaces are missing.
One must create the `oystr_execution_journal` keyspace from `schema.cql`

# How to redirect cloud clients to your local machine?
Use 9556 to run the execution service locally and receive requests from remote by remote forwarding the port using `ssh -R 0.0.0.0:9556:localhost:9556 user@server`.
Change the `ClientAcceptor` to use port `9556` only.

# Dependencies
1. Install Cassandra.
2. Create Keyspaces.
3. Add `version` column to Postgresql

# Deployment
A new docker image is build and pushed to google container registry upon successful builds

1. Pull the image: `gcloud docker -- pull us.gcr.io/oystr-containers/oystr-execution-service:$version`
2. Run the container: `docker run --name execution-service --net="host" -v /opt/oystr/server/data:/opt/oystr/server/data -d us.gcr.io/oystr-containers/oystr-execution-service:$version`
3. Test the service: `curl http://localhost:9001/executions`

# Passo a passo para inicialização da infraestrutura

## Para rodar a infraestrutura o passo a passo é:
1. Primeiramente é necessário fazer a inicialização do cassandra, caso o mesmo tenha sido rodado anteriormente, basta executar:
     * `$ docker start cassandra`
   
2. Posteriormente inicializamos o execution-service:
     * `$ docker run --name execution-service --net="host" -v /tmp:/tmp -v /opt/oystr/service/shared:/opt/oystr/service/shared -d us.gcr.io/oystr-cloud-test/oystr-execution-service:v1.0`

3. Em seguida o bot-service:
     * `$ docker run --name bot-service --net="host" -v /var/run/docker.sock:/var/run/docker.sock -v /opt/oystr/bot-service/shared:/opt/oystr/service/shared -d us.gcr.io/oystr-cloud-test/oystr-bot-service:v1.0`

4. E o rest-api:
     * `$ docker run --name rest-api  --net="host" -v /opt/oystr/rest-api/shared:/opt/oystr/service/shared -d us.gcr.io/oystr-cloud-test/oystr-rest-api:v1.0`

5. Por fim iniciamos o console:
     * `$ npm start`

## Para adicionar um robô na infraestrutura o passo a passo é:
1. Compilar o oystr-commons caso tenham modificações:
     * `$ mvn clean install`

2. Compilar o oystr-oystr caso tenham modificações:
     * `$ mvn clean install`

3. Fazer o empacotamento e gerar a imagem do bot-wrapper:
     * `$ sbt clean universal:package-bin`
     * Dentro da pasta build/: `$ ./build-docker-image.sh`

4. Compilar o bots-es caso o robô possua uma hierarquia de dependências que inclua outros projetos, do contrário, só o projeto em si (pasta projudi/ por exemplo). e.g: `bots-commons-shared, bots-commons-movimentation...`:
     * `$ mvn clean install eclipse:eclipse -Declipse.workspace=/home/$USER/eclipse-workspace (ou o path correto)`

5. Na pasta do robô, copiar as dependências para a pasta target/dependencies:
     * `$ mvn dependency:copy-dependencies`

6. Dentro da pasta build, gerar a imagem do docker:
     * `$ ./build-docker-image.sh`

7. Rodar o robô:
     * `$ docker run -d -v /opt/oystr/service/shared:/opt/oystr/service/shared -P --name=<NOME_ROBO> <NOME_ROBO>:<VERSAO>`
     * Ou se tiver adicionado as funções criadas pelo Leandro: `$ drun <NOME_ROBO>`
     
## Para realizar o debug de possíveis erros, é necessário verificar os logs:
1. Do execution-service:
     * `$ docker logs -f execution-service`

2. Do bot-service:
     * `$ docker logs -f bot-service`

3. Do rest-api:
     * `$ docker logs -f rest-api`

4. Do robô:
     * `$ docker logs -f <NOME_ROBO ou NOME_CONTAINER se for diferente>`
     * `Logs do eclipse`

5. Da execução (contém todos os logs do robô e do bot-wrapper, especialmente os logs detalhados referentes ao tratamento dos relatórios):
     * `$ tailf /opt/oystr/service/shared/logs/executions/<EXECUTION_ID>/on-TIMESTAMP.log`

# Todo
 * FilePointer location resolution [90%]
 * Bot/Queue Credentials
 * Graceful shutdown (stop all surrogates)
 * Surrogate reconnect
 * Recalculate surrogates on restart (spawn fewer depending on remaining queue size/items)
 * Abort execution (error rate)
 * Abort execution (abort from client)
 * Unit tests

# Major Changes

 * Single QueueManager (json based)
 * Single Journal (cassandra based)
 * New, simplified, Oystr Broker Client
 * Decoupled Execution Service
 * Lombok
