# Getting Started Guide

## Prerequisites

| Tool             | Minimum Version | Notes                               |
| ---------------- | --------------- | ----------------------------------- |
| Java (OpenJDK)   | 21              | LTS — required                      |
| Maven            | 3.9+            | or use the bundled `./mvnw` wrapper |
| Node.js          | 22.15.0         | required for Git hooks toolchain    |
| pnpm             | 10.32.1         | install via `npm i -g pnpm`         |
| Docker + Compose | any recent      | for running SonarQube locally       |
| Git              | any recent      |                                     |

## Quick Start

### 1. Create a new repository from this template

Click **[Use this template](https://github.com/IQKV/standard-maven-single-project-layout/generate)** on GitHub, or clone directly:

```bash
git clone https://github.com/IQKV/standard-maven-single-project-layout.git my-service
cd my-service
```

### 2. Install Git hooks

```bash
pnpm install
```

This installs Husky, Commitlint, oxfmt, Stylelint, lint-staged, and release-it via `pnpm`.

### 3. Start local development services

```bash
docker compose up -d
```

Starts a local **SonarQube 25.3 Community** instance at [http://localhost:9001](http://localhost:9001) (no forced authentication by default).

### 4. Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -P dev
```

The `dev` Maven profile adds `spring-boot-devtools`. The JVM timezone is forced to UTC by default.

## Project Customization Checklist

After creating your repository from the template:

- [ ] Update `groupId`, `artifactId`, `name`, `description` in `pom.xml`
- [ ] Update `<start-class>` property in `pom.xml` to match your new package
- [ ] Rename the Java package from `com.iqscaffold.servicename` to your own (`com.example.myservice`)
- [ ] Update `spring.application.name` in `src/main/resources/application.yml`
- [ ] Update `name` in `package.json` (`@iqkv/servicename` → `@yourorg/myservice`)
- [ ] Rename `compose.yaml` service name from `dev-servicename` to your service name
- [ ] Replace this `README.md` with `README.template.md` (the `use-template.yml` workflow does this automatically when the template is used on GitHub)

## Development Profiles

| Maven Profile   | Spring Profile | Purpose                                    |
| --------------- | -------------- | ------------------------------------------ |
| `default`       | —              | Standard build, active by default          |
| `dev`           | `dev`          | Adds `spring-boot-devtools` for hot reload |
| `production`    | —              | Placeholder for production config          |
| `modulith-test` | —              | Runs Modulith-specific tests only          |
| `use-qulice`    | —              | Enables Qulice static analysis             |

## Running Tests

```bash
# Unit + architecture tests (fast)
./mvnw clean test -Dcheckstyle.skip=true

# Modulith verification tests
./mvnw test -P modulith-test -Dcheckstyle.skip=true

# Full verify (includes JaCoCo 90% coverage gate)
./mvnw clean verify -Dcheckstyle.skip=true

# Checkstyle only
./mvnw checkstyle:check
```

## Next Steps

- Review the [Architecture Overview](03-architecture-overview.md)
- Check the [Development Workflow](04-development-workflow.md)
- Read the [Testing Guide](05-testing-guide.md)
