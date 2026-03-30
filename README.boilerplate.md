> ## 🤔 What is this template all about?
>
> - This template can be used as a base layer for a single-module Maven project.
> - Make the project easy to maintain with **7 issue templates**.
> - Quick-start documentation
> - Manage issues with **20 issue labels**.
> - Make _community healthier_ with all the guides like code of conduct, contributing, support, security...
> - Learn more with the [official GitHub guide on creating repositories from a template](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/creating-a-repository-from-a-template).
> - To start using it, click **[Use this template](https://github.com/IQKV/standard-maven-single-project-layout/generate)** to create your new repository.

---

# Spring Boot Application Template

A GitHub template for quickly bootstrapping Spring Boot applications with best practices.

## Overview

This template provides a standardized foundation for developing Java applications using Spring Boot with Maven's single-module layout. For detailed documentation, please refer to
the [template-docs](./template-docs) directory.

## Template Customization

To use this template for your own project:

1. Click the **[Use this template](https://github.com/IQKV/standard-maven-single-project-layout/generate)** button at the top of the repository
2. Name your repository and provide a description
3. Choose the repository visibility (public or private)
4. Click **Create repository from template**

After creating your repository:

1. Update the project name and description in `pom.xml`
2. Modify the package names in `src/main/java` and `src/test/java`
3. Update this README.md with your project-specific information
4. Review and adjust GitHub workflows in `.github/workflows` as needed
5. Customize environment variables in `compose.yaml` for your application

## Quick Links

- [Project Overview](./template-docs/01-project-overview.md)
- [Getting Started Guide](./template-docs/02-getting-started.md)
- [Architecture Overview](./template-docs/03-architecture-overview.md)
- [Development Workflow](./template-docs/04-development-workflow.md)
- [Testing Guide](./template-docs/05-testing-guide.md)

## Key Features

- **Project Structure**: Standard Maven single-module layout
- **GitHub Integration**: Issue templates, labels, and workflows
- **Quality Tools**: Code formatting, linting, and testing setup
- **Documentation**: Community guidelines and contribution process

## Prerequisites

- Java 21 (OpenJDK)
- Maven
- Node.js & pnpm
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/IQKV/standard-maven-single-project-layout.git my-service

# Navigate to project directory
cd my-service

# Install git hooks
pnpm install

# Start local dev services
docker compose up -d

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=local -P dev
```

For more detailed instructions, configuration options, and development guidelines, please refer to the [documentation](./template-docs/).

## License

This project is licensed under the Apache License. See the [LICENSE](LICENSE) file for details.

## Contributing

Please read our [Contributing Guidelines](.github/CONTRIBUTING.md) and [Code of Conduct](.github/CODE_OF_CONDUCT.md).
