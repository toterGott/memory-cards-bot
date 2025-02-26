```shell
docker network create tg_bot_network
```
Docker run Postgresql

```shell
docker run -d \
  --name postgres_container \
  -e POSTGRES_USER=memory_cards \
  -e POSTGRES_PASSWORD=local_password \
  -e POSTGRES_DB=memory_cards \
  --network tg_bot_network \
  -v ~/postgresql_data:/var/lib/postgresql/data \
  -p 127.0.0.1:5432:5432 \
  postgres:17.3
```

## Build and run the container locally

```shell
./gradlew clean build
docker build -t memory-cards:latest .
```

```shell
docker run -d \
  --name memory-cards \
  --network tg_bot_network \
  -e BOT_TOKEN= \
  memory-cards:latest
```

## Grafana
```shell
ssh -L 3000:localhost:3000 ubuntu@bot-server
```

## Prometheus 
```shell
ssh -L 9090:localhost:9090 ubuntu@bot-server
```

## Loki
```shell
ssh -L 3100:localhost:3100 ubuntu@bot-server
```

## Node exporter
```shell
ssh -L 9100:localhost:9100 ubuntu@bot-server
```