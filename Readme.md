# Memory cards bot 

This is a telegram bot with memory cards.

 ## TODOs
- Add Grafana loki for logs
- Add liquibase
- Todo make cards unavailable if it's not time yet (should I?)
- Add "create a card" in collection's options
- Change default collections names on a language change


```shell
docker network create tg_bot_network
```

```shell
docker run -d \
  --name postgres_container \
  -e POSTGRES_USER=memory_cards \
  -e POSTGRES_PASSWORD=local_password \
  -e POSTGRES_DB=memory_cards \
  --network tg_bot_network \
  -v ~/postgresql_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:latest
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
