[< Back](../README.md)
---

## Building

```
./gradlew build
```

## Running

The DPS Data Compliance Service, a PostgreSQL database, Elite2 API 
and Localstack docker containers can be run by:

```
TMPDIR=/private$TMPDIR docker-compose up
```

Without Docker, the service can be run locally by:
```
./gradlew bootRun
```
as long as the environment variables below are specified:

### Environment Variables

If running locally, the service has the following minimum environment variables provided 
to start up:

- ELITE2_API_BASE_URL (URL for the Elite2 API)
- OAUTH2_API_BASE_URL (URL for the OAuth2 server)
- APP_DB_URL (URL for a database instance to store data compliance data)

And if the database instance has username / password protection:
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD

