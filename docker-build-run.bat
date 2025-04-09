docker build -t temp-messages .
docker run -d --network temp-messages_my-network -p 8080:8080 temp-messages