[< Back](../README.md)
---

## Building

To build (append `-x test` to build without running tests):
```
./gradlew clean build
```
To rebuild the docker image locally (perhaps after changes to the project), run:
```
docker build -t mojdigitalstudio/dps-data-compliance:latest .
```

### Common gradle tasks 
To list project dependencies, run:
```
./gradlew dependencies
```

To check for dependency updates, run:
```
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```
./gradlew clean dependencyCheckAnalyze --info
```

To upgrade the gradle wrapper version, run:
```
./gradlew wrapper --gradle-version=<VERSION>
```
To automatically update project dependencies, run:
```
./gradlew useLatestVersions
```
## Running

The DPS Data Compliance Service, a PostgreSQL database, HMPPS Auth, Prison API 
and Localstack docker containers can be run by:
```
TMPDIR=/private$TMPDIR docker-compose up
```

Without Docker, the service can be run locally by:

```
./gradlew bootRun --args='--spring.profiles.active=dev'
```
which will run a reduced service with an in-memory hsqldb database and without
any connection to AWS or Localstack. The `application-dev.yml` properties file
expects that the following instances are running:

- HMPPS Auth : `localhost:8081`
- Prison API : `localhost:8082`
- Path Finder API : `localhost:8083`
- Community API : `localhost:8084`
- Prison-Register API: `localhost:8085`

More details on the required environment variables
are provided below.

The service can be run in a similar way within IntelliJ by running the main class with the following VM options:
```
-Dspring.profiles.active=dev
```

### Environment Variables

If running locally, the service has the following minimum environment variables provided 
to start up:

- `PRISON_API_BASE_URL` (URL for the Prison API)
- `HMPPS_AUTH_BASE_URL` (URL for the HMPPS Auth server)
- `PATHFINDER_API_BASE_URL` (URL for the Pathfinder API)
- `COMMUNITY_API_BASE_URL` (URL for the Community API)
- `PRISON_REGISTER_BASE_URL` (URL for the Prison-Register API)
- `APP_DB_URL` (URL for a database instance to store data compliance data)
- `OFFENDER_RETENTION_IMAGE_DUPLICATE_CHECK_ENABLED` (Flag to use AWS Rekognition to perform duplicate image check)
- `OFFENDER_RETENTION_DATA_DUPLICATE_ID_CHECK_ENABLED` (Flag to use the Prison API to perform a duplicate ID check)
- `OFFENDER_RETENTION_DATA_DUPLICATE_DB_CHECK_ENABLED` (Flag to use the Prison API to perform a duplicate check using data in the database)
- `OFFENDER_RETENTION_DATA_DUPLICATE_AP_CHECK_ENABLED` (Flag to query Analytical Platform for potential duplicates)

These are provided already if running with the `dev` Spring profile (see the `application-dev.yml` properties file).

If the database instance has username / password protection the following are also required:
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
