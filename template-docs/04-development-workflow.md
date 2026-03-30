# Development Workflow

## Git Workflow

### Branching Strategy

| Branch          | Purpose                                |
| --------------- | -------------------------------------- |
| `main`          | Production-ready code                  |
| `develop`       | Integration branch (optional)          |
| `feature/*`     | New features                           |
| `bugfix/*`      | Bug fixes                              |
| `improvement/*` | Enhancements to existing functionality |
| `hotfix/*`      | Production emergency fixes             |
| `rfc/*`         | Architectural proposals                |

### Commit Convention

All commits must follow [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): subject
```

**Allowed types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `build`, `ci`, `revert`, `improvement`, `rfc`

**Examples:**

```
feat(user): add registration endpoint
fix(auth): correct token expiry calculation
improvement(logging): switch to structured JSON output
```

Commit messages are validated by **Commitlint** on every `git commit` (via Husky) and by the `check-commit-message.yml` CI workflow on every push.

PR titles are validated by `check-pr-title.yml` using the same Conventional Commits rules.

### Pre-commit Hooks (Husky + lint-staged)

On every `git commit`, `lint-staged` runs:

- **oxfmt** — formats all staged files (Java, JSON, YAML, Markdown, etc.)
- **sort-package-json** — keeps `package.json` keys sorted
- **Stylelint** — lints `*.css` / `*.scss` files

Run the full lint suite manually:

```bash
pnpm lint           # stylelint + oxfmt check
pnpm formatter:write  # auto-fix formatting
```

## Development Cycle

### 1. Setup

```bash
pnpm install                         # install Git hooks
docker compose up -d                 # start local SonarQube
```

### 2. Implement Changes

- Add code following [Architecture Overview](03-architecture-overview.md) layer structure
- Write tests alongside new functionality
- Use Java 21 features: records, pattern matching, text blocks, `var`

### 3. Run Tests Locally

```bash
# Unit + architecture tests
./mvnw clean test -Dcheckstyle.skip=true

# Modulith verification
./mvnw test -P modulith-test -Dcheckstyle.skip=true

# Full build with JaCoCo coverage gate (90%)
./mvnw clean verify -Dcheckstyle.skip=true
```

### 4. Code Quality

```bash
# Run Checkstyle explicitly
./mvnw checkstyle:check

# Generate JaCoCo HTML report
./mvnw jacoco:report
# → open target/site/jacoco/index.html
```

### 5. Submit Changes

- Push branch, open a pull request
- PR title must follow Conventional Commits format
- All CI checks must pass before merging

## Continuous Integration (GitHub Actions)

| Workflow                         | Trigger                   | What it does                                  |
| -------------------------------- | ------------------------- | --------------------------------------------- |
| `check-commit-message.yml`       | push                      | Validates all commit messages via Commitlint  |
| `check-pr-title.yml`             | pull_request              | Validates PR title format                     |
| `build-nodejs-project.yml`       | push / PR                 | Runs `pnpm lint` (oxfmt + Stylelint)          |
| `check-markdown-links.yml`       | push / PR                 | Validates all Markdown links                  |
| `auto-approve-dependabot-pr.yml` | pull_request              | Auto-approves Dependabot dependency updates   |
| `use-template.yml`               | push (non-template repos) | Bootstraps new repo after "Use this template" |

> **Note:** There is no Java Maven build CI workflow in the template itself — add one per the project's own pipeline requirements.

## Release Process

Releases are managed via `release-it` with `@release-it/conventional-changelog`:

```bash
pnpm release            # interactive release
pnpm release --ci       # non-interactive (CI mode)
```

This will:

1. Bump the version in `package.json` based on commit types
2. Generate/update `CHANGELOG.md`
3. Create a git tag
4. Push tag and release commit

> Update `pom.xml` version separately before or after the `release-it` run.

## Dependency Management

- **Java dependencies**: managed via `com.iqkv:boot-parent-pom` BOM — no explicit versions needed for managed artifacts
- **Node.js dependencies**: `pnpm` with lockfile (`pnpm-lock.yaml`) — always commit the lockfile
- **Automated updates**: Dependabot is configured to suggest updates for both Maven and npm ecosystems
