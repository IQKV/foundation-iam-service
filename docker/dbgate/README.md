# DbGate Configuration - IAM Service

This directory contains the DbGate database administration tool configuration for the IAM Service local development environment.

## Pre-configured Connections

The `connections.jsonl` file contains pre-configured connections for all IAM service infrastructure:

### 1. PostgreSQL - IAM Service

- **ID**: `postgres-iam`
- **Server**: `postgres-iam:5432`
- **Database**: `iam`
- **User**: `svc_iam_dba`
- **Engine**: `postgres@dbgate-plugin-postgres`

### 2. RabbitMQ - IAM Service

- **ID**: `rabbitmq-management`
- **Server**: `rabbitmq-iam:15672`
- **User**: `svc_iam_rmq`
- **Engine**: `rabbitmq@dbgate-plugin-rabbitmq`

### 3. MinIO - S3 Storage

- **ID**: `minio-s3`
- **Server**: `minio:9000`
- **User**: `iqkv`
- **Engine**: `s3@dbgate-plugin-s3`

## Usage

### Start Infrastructure with DbGate

```bash
# Start infrastructure only (for IDE development)
docker compose up -d

# Access DbGate at: http://localhost:3000
```

### Start Full Stack with DbGate

```bash
# Start infrastructure + IAM service
docker compose -f compose.container.yaml up -d

# Access DbGate at: http://localhost:3000
```

### Access DbGate UI

Once the services are running, open your browser to:

**http://localhost:3000**

All connections are pre-configured and ready to use.

## What You Can Do with DbGate

### PostgreSQL Database

- Browse schemas, tables, and views
- Execute SQL queries with syntax highlighting
- Import/export data (CSV, JSON, SQL)
- View and edit table data
- Analyze table structure and relationships

### RabbitMQ

- View queues and their message counts
- Monitor exchange bindings
- Check connection statistics
- View queue configurations

### MinIO S3 Storage

- Browse buckets and objects
- Upload and download files
- Delete objects
- View object metadata

## Updating Credentials

If you change credentials in your `.env` or compose files, update `connections.jsonl`:

```json
{
    "_id": "postgres-iam",
    "engine": "postgres@dbgate-plugin-postgres",
    "server": "postgres-iam",
    "port": 5432,
    "user": "NEW_USER",
    "password": "NEW_PASSWORD",
    "database": "iam",
    "displayName": "PostgreSQL - IAM Service"
}
```

Then restart DbGate:

```bash
docker compose restart dbgate
```

## Troubleshooting

### DbGate won't start

```bash
# Check logs
docker logs foundation-iam-dbgate-dev

# Verify connections file exists
ls docker/dbgate/connections.jsonl

# Restart DbGate
docker compose restart dbgate
```

### Can't connect to PostgreSQL

```bash
# Verify PostgreSQL is running and healthy
docker ps --filter name=foundation-iam-postgres-dev

# Check PostgreSQL logs
docker logs foundation-iam-postgres-dev

# Test connection from DbGate container
docker exec foundation-iam-dbgate-dev ping postgres-iam
```

### Can't connect to RabbitMQ

```bash
# Verify RabbitMQ is running
docker ps --filter name=foundation-iam-rabbitmq-dev

# Check RabbitMQ logs
docker logs foundation-iam-rabbitmq-dev

# Ensure management plugin is enabled (it's enabled by default in our image)
```

### Can't connect to MinIO

```bash
# Verify MinIO is running
docker ps --filter name=foundation-iam-minio-dev

# Check MinIO logs
docker logs foundation-iam-minio-dev

# Test MinIO health
docker exec foundation-iam-minio-dev mc ready local
```

### Reset DbGate Data

```bash
# Stop and remove DbGate
docker compose stop dbgate
docker compose rm -f dbgate

# Remove volume
docker volume rm iqkv_iam_dbgate_data_dev

# Restart
docker compose up -d dbgate
```

## Security Notes

⚠️ **Important**: This configuration is for local development only.

- Credentials are stored in plaintext in `connections.jsonl`
- Do not commit real production credentials to version control
- DbGate port 3000 is exposed to localhost only
- Authentication is enabled (`LOGINS=1`)

## Adding Custom Connections

To add additional database connections, append a new line to `connections.jsonl`:

```json
{
    "_id": "my-custom-db",
    "engine": "postgres@dbgate-plugin-postgres",
    "server": "hostname",
    "port": 5432,
    "user": "username",
    "password": "password",
    "database": "dbname",
    "displayName": "My Custom Database"
}
```

### Supported Engines

- `postgres@dbgate-plugin-postgres` - PostgreSQL
- `mysql@dbgate-plugin-mysql` - MySQL
- `mariadb@dbgate-plugin-mariadb` - MariaDB
- `mongo@dbgate-plugin-mongo` - MongoDB
- `redis@dbgate-plugin-redis` - Redis
- `rabbitmq@dbgate-plugin-rabbitmq` - RabbitMQ
- `s3@dbgate-plugin-s3` - S3-compatible storage

## Compose File Configuration

DbGate is included in:

- ✅ `compose.yaml` - Infrastructure only (use with IDE)
- ✅ `compose.container.yaml` - Full stack (infrastructure + service)
- ✅ `compose.base.yaml` - Base definitions

All three configurations include DbGate by default.

## More Information

- [DbGate Official Documentation](https://dbgate.org/docs/)
- [DbGate GitHub Repository](https://github.com/dbgate/dbgate)
- [Supported Database Engines](https://dbgate.org/docs/databases.html)

## Tips

1. **Query History**: DbGate automatically saves your SQL query history
2. **Keyboard Shortcuts**: Press `Ctrl+Enter` to execute queries
3. **Export Data**: Right-click tables to export as CSV, JSON, or SQL
4. **Dark Mode**: Available in settings (top-right corner)
5. **Schema Diagram**: View ER diagrams to understand table relationships
