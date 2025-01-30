# Memory cards bot 

This is a telegram bot with memory cards.

 ## TODOs
- FIX change collection on get card
- FIX change collection on create create

- Split CardHandler into CardScreenHandler and CollectionCardHandler
- Add emojis in buttons in collections buttons
- Enable force mode for 10 m even if there are no cards for now 
- Add liquibase
- Add "create a card" in collection's options
- Add Grafana loki for logs
- Add some tests
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
