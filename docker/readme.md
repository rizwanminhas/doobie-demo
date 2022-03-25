1. navigate to ./docker and execute `docker-compose up`
2. open a new terminal and execute `docker ps`.
3. find the name of the postgres container and then connect to it using
`docker exec -it docker_db_1 bash`.
4. connect to the db using `psql -U docker -d myimdb`
5. execute a sql query like `select * from actors;` to make sure everything is setup correctly.