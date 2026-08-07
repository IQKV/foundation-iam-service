## 📜 Deployment Guide

### Overview

The Foundation IAM Service is deployed using Helm charts and automated CI/CD pipelines. The service provides JWT RS256-based authentication, multi-tenancy, RBAC, and email verification capabilities.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (PostgreSQL, RabbitMQ)

### Environments

| Environment | Namespace      | Purpose                     |
| ----------- | -------------- | --------------------------- |
| Test        | `iqkv-sit-env` | Feature branch testing      |
| Staging     | `iqkv-uat-env` | Pre-production validation   |
| Production  | `iqkv-prd-env` | Live production environment |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

<details>
<summary>📋 Pipeline Stages</summary>

The service uses Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgress** - WIP branch auto-deployment
5. **RollbackWorkInProgress** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion to SIT
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion to UAT/PRD
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

</details>

<details>
<summary>🔐 Required Drone Secrets</summary>

| Secret Name                       | Purpose                           | Used In                                    |
| --------------------------------- | --------------------------------- | ------------------------------------------ |
| `NEXUS_DEPLOYER_USERNAME`         | Nexus repository authentication   | Artifact publishing, dependency resolution |
| `NEXUS_DEPLOYER_PASSWORD`         | Nexus repository authentication   | Artifact publishing, dependency resolution |
| `SONAR_HOST`                      | SonarQube server URL              | Static code analysis                       |
| `SONAR_TOKEN`                     | SonarQube authentication token    | Static code analysis                       |
| `SLACK_WEBHOOK`                   | Slack notifications webhook URL   | Build status notifications                 |
| `GITHUB_API_ACCESS_TOKEN`         | GitHub API access for releases    | Release creation, changelog generation     |
| `SVC_CONTAINER_REGISTRY_USERNAME` | Container registry authentication | Docker image publishing                    |
| `SVC_CONTAINER_REGISTRY_PASSWORD` | Container registry authentication | Docker image publishing                    |
| `HELM_CHARTS_REPOSITORY`          | Helm charts repository URL        | Kubernetes deployments                     |
| `INFRA_POSTGRESQL_PASSWORD`       | PostgreSQL database password      | Application configuration                  |
| `INFRA_RABBITMQ_PASSWORD`         | RabbitMQ message broker password  | Application configuration                  |
| `SMTP_USERNAME`                   | Email service username            | Email notifications                        |
| `SMTP_PASSWORD`                   | Email service password            | Email notifications                        |

</details>

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ SIT      | -              | SIT                |
| `feature/*` | -           | ✅ SIT         | SIT                |
| Tags        | -           | ✅ UAT / PRD   | UAT / Production   |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

<details>
<summary>Helm Commands</summary>

```bash
# WIP / Feature branches (SIT)
helm upgrade --install --rollback-on-failure --wait --timeout 5m foundation-iam-service ./ \
  --values ./values.yaml \
  --values ./values-sit.yaml \
  --set image.tag=${DRONE_BRANCH} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.mail.username=${SMTP_USERNAME} \
  --set config.mail.password=${SMTP_PASSWORD} \
  --namespace iqkv-sit-env \
  --create-namespace

# Production (tagged releases)
helm upgrade --install --rollback-on-failure --wait --timeout 5m foundation-iam-service ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --set image.tag=${DRONE_TAG} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.mail.username=${SMTP_USERNAME} \
  --set config.mail.password=${SMTP_PASSWORD} \
  --namespace iqkv-prd-env \
  --create-namespace
```

</details>

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/foundation-iam-service

# Deploy to SIT
helm upgrade --install foundation-iam-service ./ \
  --values values-sit.yaml \
  --set infraServices.postgresql.password="your-db-password" \
  --set infraServices.rabbitmq.password="your-rabbitmq-password" \
  --set config.mail.username="your-smtp-username" \
  --set config.mail.password="your-smtp-password" \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Secret Configuration Examples

```bash
# Infrastructure secrets
drone secret add --repository IQKV/foundation-iam-service --name INFRA_POSTGRESQL_PASSWORD --data "your-postgresql-password"
drone secret add --repository IQKV/foundation-iam-service --name INFRA_RABBITMQ_PASSWORD --data "your-rabbitmq-password"

# Application secrets
drone secret add --repository IQKV/foundation-iam-service --name SMTP_USERNAME --data "your-smtp-username"
drone secret add --repository IQKV/foundation-iam-service --name SMTP_PASSWORD --data "your-smtp-password"
```

#### External Services

The service connects to these external infrastructure components:

- **PostgreSQL**: Identity data storage (users, tenants, authortities, permissions)
- **RabbitMQ**: Event messaging and inter-service communication
- **SMTP Server**: Email verification and notification delivery

> **SIT/UAT**: SMTP is handled by an in-cluster MailHog instance. Production uses `smtp.iqkv.site`.

#### Service Configuration

| Setting        | SIT      | UAT      | Production    |
| -------------- | -------- | -------- | ------------- |
| Replicas       | 1        | 1        | 2             |
| CPU Request    | 300m     | 500m     | 500m          |
| CPU Limit      | 750m     | 1000m    | 1000m         |
| Memory Request | 384Mi    | 512Mi    | 512Mi         |
| Memory Limit   | 768Mi    | 1Gi      | 1Gi           |
| Autoscaling    | Disabled | Disabled | 2–10 replicas |
| Ingress        | Disabled | Disabled | Configurable  |
| Monitoring     | Disabled | Enabled  | Enabled       |
| Network Policy | Disabled | Disabled | Enabled       |

#### Platform Rollout Mode

The `platform.rolloutMode` value controls multi-tenancy behavior. Valid values: `MULTI_TENANT` (default) | `SINGLE_TENANT`.

> **Important**: This value must be identical across `foundation-iam-service`, `foundation-billing-service`, and `foundation-gateway-service`. Mixed modes are a hard deployment error.

### Monitoring & Health Checks

#### Health Endpoints

- **Liveness**: `/actuator/health/liveness` (port 8081)
- **Readiness**: `/actuator/health/readiness` (port 8081)
- **Metrics**: `/actuator/prometheus` (port 8081)

#### Monitoring Stack

UAT and production deployments include:

- Prometheus ServiceMonitor
- Alerting rules: service down, high memory (>85%), high CPU (>80%), high error rate (>5% 5xx), slow responses (p95 >2s)
- Grafana dashboards

### Troubleshooting

#### Common Issues

1. **Database Connection Failures**

    ```bash
    kubectl logs deployment/foundation-iam-service -n iqkv-sit-env
    ```

2. **RabbitMQ Connection Issues**

    ```bash
    kubectl describe pod -l app.kubernetes.io/name=foundation-iam-service -n iqkv-sit-env
    ```

3. **Email / SMTP Configuration**

    ```bash
    kubectl get secret foundation-iam-service-secrets -n iqkv-sit-env -o yaml
    ```

4. **Check Configuration**

    ```bash
    kubectl describe configmap foundation-iam-service-config -n iqkv-sit-env
    kubectl describe secret foundation-iam-service-secrets -n iqkv-sit-env
    ```

5. **Test Health Endpoints**

    ```bash
    kubectl port-forward deployment/foundation-iam-service 8081:8081 -n iqkv-sit-env
    curl http://localhost:8081/actuator/health
    ```

#### Missing Secrets Diagnosis

```bash
# List all secrets in namespace
kubectl get secrets -n iqkv-sit-env

# Check specific secret content
kubectl get secret foundation-iam-service-secrets -n iqkv-sit-env -o yaml

# Verify Drone CI secrets are configured
drone secret ls --repository IQKV/foundation-iam-service
```

#### Rollback

```bash
# Rollback to previous Helm revision
helm rollback foundation-iam-service -n iqkv-prd-env

# Or uninstall completely
helm uninstall foundation-iam-service -n iqkv-prd-env
```

### Security

- All sensitive values injected via `--set` flags from Drone secrets (never stored in chart)
- JWT uses RS256 asymmetric keys; production keys loaded from `file:/run/secrets/`
- TLS configurable via cert-manager in production ingress
- Network policies restrict pod communication in production
- Non-root container execution (UID 1001)
- Read-only root filesystem in production
