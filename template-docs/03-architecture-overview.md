# Architecture Overview

## Application Structure

This template follows a **Tactical DDD** layout with hexagonal architecture, enforced at test time by ArchUnit (`TechnicalStructureTest`):

```
src/main/java/com/iqkv/{servicename}/
├── shared/                   # Shared kernel (cross-cutting concerns)
│   ├── domain/               # Shared domain primitives
│   ├── exception/            # Common exceptions
│   └── util/                 # Utility classes
├── infrastructure/           # Infrastructure layer — top layer, may not be accessed by others
│   ├── config/               # Spring configuration
│   ├── security/             # Security implementation
│   ├── persistence/          # JPA repositories, adapters
│   └── messaging/            # Event publishers, message brokers
├── [bounded-context]/        # e.g. user, order, payment
│   ├── domain/               # Domain layer — core business logic
│   │   ├── model/            # Aggregates, entities, value objects
│   │   ├── service/          # Domain services
│   │   └── event/            # Domain events
│   ├── application/          # Application layer — use cases
│   │   ├── service/          # Application services
│   │   ├── dto/              # Data transfer objects
│   │   └── port/             # Ports (interfaces for adapters)
│   └── adapter/              # Adapters — interface layer
│       ├── in/
│       │   └── rest/         # REST controllers (inbound)
│       └── out/
│           └── persistence/  # Repository implementations (outbound)
└── {Servicename}Application.java   # Entry point (UTC timezone, @SpringBootApplication)
```

**Layer access rules** (enforced by `TechnicalStructureTest.java` on every build):

| Layer                       | May be accessed by                 |
| --------------------------- | ---------------------------------- |
| `infrastructure`            | may not be accessed by any layer   |
| `adapter.in` (web)          | only by `infrastructure`           |
| `application`               | by `adapter.in`, `infrastructure`  |
| `adapter.out` (persistence) | by `application`, `infrastructure` |
| `domain`                    | by all layers above                |
| `shared`                    | by all layers                      |

## Spring Modulith

The project includes `spring-modulith-starter-core`, which enables:

- **Module boundary detection** — packages directly under the root package are treated as application modules
- **Module verification** (`ModulithTest`) — asserts no unwanted cross-module dependencies
- **Integration testing by module** (`ModulithIntegrationTest`)
- **Scenario testing** (`ModulithScenarioTest`) — event-based workflow testing
- **Runtime actuator endpoint** — `spring-modulith-actuator` exposes module metadata at `/actuator/modulith`

Run Modulith-specific tests with:

```bash
./mvnw test -P modulith-test
```

## Key Components

| Component             | Technology                         | Notes                               |
| --------------------- | ---------------------------------- | ----------------------------------- |
| Application framework | Spring Boot 3.x                    | Via `com.iqkv:boot-parent-pom`      |
| Module system         | Spring Modulith                    | Module verification + actuator      |
| Persistence           | Spring Data JPA                    | Added per-service as needed         |
| Logging               | Logback + logstash-logback-encoder | Structured JSON output              |
| Metrics               | Micrometer + Prometheus            | Scraped by `/actuator/prometheus`   |
| Build info            | `git-commit-id-maven-plugin`       | Exposed via `/actuator/info`        |
| Architecture testing  | ArchUnit (JUnit 5)                 | `TechnicalStructureTest`            |
| Coverage              | JaCoCo                             | 90% min instruction / line / branch |
| Code style            | Checkstyle                         | Config managed in parent POM        |

## Configuration Management

| File                                     | Purpose                                                       |
| ---------------------------------------- | ------------------------------------------------------------- |
| `src/main/resources/application.yml`     | Base config; git build metadata placeholders                  |
| `src/main/resources/application-dev.yml` | Dev profile overrides (devtools, relaxed settings)            |
| `src/main/resources/logback-spring.xml`  | JSON structured logging via logstash encoder                  |
| `compose.yaml`                           | Local SonarQube 25.3 instance                                 |
| `pom.xml` profiles                       | `default`, `dev`, `production`, `modulith-test`, `use-qulice` |

`maven-resources-plugin` uses `@` as the resource filter delimiter to resolve `@project.version@` and `@git.commit.id.abbrev@` into `application.yml` at build time.

## Build Info / Actuator

`/actuator/info` exposes:

```json
{
    "app": {
        "name": "<project.name>",
        "description": "<project.description>",
        "version": "<project.version>",
        "git_commit": "<short SHA>",
        "build_time": "<ISO timestamp>"
    }
}
```

## Security Considerations

- No security starter is included in the template — add `spring-boot-starter-security` and configure for your service
- Checkstyle rules (via parent POM) enforce secure coding patterns
- SonarQube (`compose.yaml`) provides SAST analysis during development

## Extensibility

Common additions to this template baseline:

- `spring-boot-starter-web` or `spring-boot-starter-webflux` — REST API
- `spring-boot-starter-data-jpa` + JDBC driver — persistence
- `spring-boot-starter-security` + JWT — authentication
- `spring-boot-starter-validation` — Bean Validation
- `springdoc-openapi-starter-webmvc-ui` — Swagger/OpenAPI docs
- `spring-boot-starter-cache` + `spring-boot-starter-data-redis` — caching
