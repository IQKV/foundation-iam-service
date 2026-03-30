# Testing Guide

## Testing Strategy

The template follows the [Test Pyramid](https://martinfowler.com/bliki/TestPyramid.html) and ships with five pre-built test categories:

| Test class                     | Type                 | Purpose                                      |
| ------------------------------ | -------------------- | -------------------------------------------- |
| `ServicenameApplicationTests`  | Smoke                | Spring context loads without errors          |
| `TechnicalStructureTest`       | Architecture         | ArchUnit enforces layered architecture rules |
| `ModulithTest`                 | Modulith             | Verifies Spring Modulith module boundaries   |
| `ModulithIntegrationTest`      | Modulith integration | Tests cross-module interactions              |
| `ModulithScenarioTest`         | Modulith scenario    | Event-driven workflow testing                |
| `IntegrationTest` (base class) | Integration          | Base class for service integration tests     |

## Running Tests

### Unit + Architecture Tests (default)

```bash
./mvnw clean test -Dcheckstyle.skip=true
```

Runs everything **except** Modulith-scoped tests. Includes `TechnicalStructureTest` (ArchUnit).

### Modulith Tests Only

```bash
./mvnw test -P modulith-test -Dcheckstyle.skip=true
```

The `modulith-test` Maven profile configures Surefire to include only:

- `**/*ModulithTest.java`
- `**/*ModulithIntegrationTest.java`
- `**/*ModulithScenarioTest.java`

### Full Build with Coverage Gate

```bash
./mvnw clean verify -Dcheckstyle.skip=true
```

Runs all tests and enforces the JaCoCo coverage minimum (see below). `*Application.class` is excluded from coverage measurement.

### Coverage Report

```bash
./mvnw jacoco:report
```

Report location: `target/site/jacoco/index.html`

## Coverage Requirements

Configured in `pom.xml` via `jacoco-maven-plugin`. JaCoCo is **skipped by default** (`jacoco.skip=true`) — enable it explicitly when needed:

```bash
./mvnw clean verify -Djacoco.skip=false -Dcheckstyle.skip=true
```

| Scope  | Counter                   | Minimum |
| ------ | ------------------------- | ------- |
| Bundle | Instruction covered ratio | **90%** |
| Bundle | Missed class count        | **0**   |
| Class  | Line covered ratio        | **90%** |
| Class  | Branch covered ratio      | **90%** |

> These are strict thresholds. Adjust in `pom.xml` under `jacoco-maven-plugin` → `coverage-check` execution if your service legitimately cannot reach 90% at early stages.

## Architecture Tests (ArchUnit)

`TechnicalStructureTest` enforces the following layer access rules (all layers are **optional** — unused layers don't cause failures):

```
infrastructure   → may not be accessed by any layer
adapter.in       → only accessed by infrastructure
application      → only accessed by adapter.in, infrastructure
adapter.out      → only accessed by application, infrastructure
domain           → only accessed by adapter.out, application, adapter.in, infrastructure
shared           → accessed by all layers
```

`ServicenameApplication` is excluded from all access checks.

These rules run automatically on every `mvn test` invocation — no separate step needed.

## Spring Modulith Tests

| Class                     | What it verifies                                                                                    |
| ------------------------- | --------------------------------------------------------------------------------------------------- |
| `ModulithTest`            | All application modules conform to Spring Modulith conventions (no illegal cross-module references) |
| `ModulithIntegrationTest` | Module interactions via published/consumed events work correctly                                    |
| `ModulithScenarioTest`    | End-to-end scenario testing using Spring Modulith's `Scenario` API                                  |

Reference: [Spring Modulith documentation](https://docs.spring.io/spring-modulith/docs/current/reference/html/)

## Unit Testing Standards

### Naming

Use descriptive method names; the template uses the `should + condition` style:

```java
@Test
@DisplayName("Should load application context")
void shouldLoadApplicationContext() { ... }
```

The alternative `methodName_scenario_expectedBehavior` pattern is also acceptable.

### Structure — Arrange / Act / Assert

```java
@Test
void shouldCreateUser() {
    // Arrange
    var command = new UserCreateCommand("john.doe", "john@example.com");
    when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // Act
    var result = userService.createUser(command);

    // Assert
    assertThat(result.username()).isEqualTo("john.doe");
    verify(userRepository).save(any(User.class));
}
```

### Mocking

- Use `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks` for pure unit tests
- Use `@MockBean` only when a Spring context is required (prefer pure Mockito to keep tests fast)
- Prefer constructor injection — it makes dependencies explicit and simplifies mocking

## Integration Tests

Add `@SpringBootTest` + `@ActiveProfiles("test")` for full-context integration tests. Use Testcontainers for any external services (databases, message brokers):

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class MyServiceIntegrationTest extends IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## Continuous Integration

Tests run automatically on:

- Every push (commit message validation + Node.js lint)
- Every pull request (PR title validation + Markdown link check)

Add a Maven build CI workflow to `.github/workflows/` for your service's own pipeline (the template intentionally omits a Java CI workflow since parent-POM-based projects typically inherit pipeline configs).
