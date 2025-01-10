# Memory cards bot 

This is a telegram bot with memory cards.

 ## TODOs
- On schedule do not send if not in stand by
- On schedule do not send if previous card not answered
- Add a default 

```shell
docker run --hostname=fb1f84960dc1 --mac-address=02:42:ac:11:00:02 --env=POSTGRES_USER=memory_cards --env=POSTGRES_PASSWORD=local_password --env=POSTGRES_DB=memory_cards --env=PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/lib/postgresql/14/bin --env=GOSU_VERSION=1.14 --env=LANG=en_US.utf8 --env=PG_MAJOR=14 --env=PG_VERSION=14.5-1.pgdg110+1 --env=PGDATA=/var/lib/postgresql/data --volume=/var/lib/postgresql/data --network=bridge -p 5432:5432 --restart=no --runtime=runc -d postgres:latest
```
```shell
sudo docker run -d \
  --name postgres_container \
  -e POSTGRES_USER=memory_cards \
  -e POSTGRES_PASSWORD=local_password \
  -e POSTGRES_DB=memory_cards \
  -v ~/postgresql_data:/var/lib/postgresql/data \
  -p 5432:5432 \
  postgres:latest
```