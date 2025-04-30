# SWIFTcode
## Running the Application

You can run the application in two main ways: locally on your machine or using Docker containers.

### Locally (without Docker)

This method requires manual setup of the environment, including the database.

1.  **Prerequisites:**
    *   Java  17 or higher
    *   Maven or Gradle.
    *   An available and running instance of PostgreSQL.
    *   The `swift_codes.csv`(Orginal name "Interns_2025_SWIFT_CODES.csv") file in `src/main/resources/`.

2.  **Configuration:**
    *   Create a database in PostgreSQL (e.g., named `swiftcodes_db`).
    *   Configure the connection to this database in the `src/main/resources/application.properties` file. Ensure that the `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` properties are correct for your local database instance.
    *   Set `spring.jpa.hibernate.ddl-auto` to `update` or `create` to allow Hibernate to manage the database schema on the first run.

3.  **Build and Run:**
    *   Open a terminal in the project's root directory (`SWIFTcode`).
    *   Run the application using Maven:
        ```bash
        mvn spring-boot:run
        ```
    *   Or using Gradle (if applicable):
        ```bash
        gradle bootRun
        ```

4.  **Access:** The application should be available at `http://localhost:8080` 

### Using Docker

This method is recommended as it automatically configures and runs both the application and the required database in isolated containers.

1.  **Prerequisites:**
    *   Docker or Docker Compose.

2.  **Build the Docker Image:**
    *   Open a terminal in the project's root directory (`SWIFTcode`).
    *   Build the Docker image for the application (this is only required for the first build or after changes to the source code or `Dockerfile`):
        ```bash
        docker build -t swift-app:latest .
        ```

3.  **Run the Containers:**
    *   In the same project root directory, start the services defined in `docker-compose.yml`:
        ```bash
        docker-compose up -d
        ```
    *   This command will:
        *   Pull the PostgreSQL image (if not already present locally).
        *   Start a PostgreSQL database container, configuring the user and password according to `docker-compose.yml`. Data will be persisted in the `postgres_data` volume.
        *   Start your Spring Boot application container (using the built `swift-app:latest` image), configuring the database connection via environment variables to link to the database container.

4.  **Access:** The application should be available at `http://localhost:8080` (or another port mapped in the `ports` section for the `app` service in `docker-compose.yml`).

5.  **Stop the Containers:**
    *   To stop the containers launched by `docker-compose`:
        ```bash
        docker-compose down
        ```
    *   If you also want to remove the database data volume (you will lose all data!):
        ```bash
        docker-compose down -v
        ```
