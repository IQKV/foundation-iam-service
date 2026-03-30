package com.iqscaffold.iam.tenancy;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "iqscaffold.liquibase.tenant-runner-enabled", havingValue = "true", matchIfMissing = true)
public class TenantLiquibaseRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantLiquibaseRunner.class);

    private static final String SYSTEM_CHANGELOG = "db/changelog/system/db.changelog-master.xml";
    private static final String TENANT_CHANGELOG = "db/changelog/tenant/master.xml";

    private final DataSource dataSource;

    public TenantLiquibaseRunner(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {
        log.info("Running system schema migrations");
        runMigrations("public", SYSTEM_CHANGELOG);
        log.info("System schema migrations complete");
    }

    public void runMigrationsForTenant(final String tenantKey) throws Exception {
        final String schema = "t_" + tenantKey;
        log.info("Running tenant schema migrations for schema: {}", schema);
        runMigrations(schema, TENANT_CHANGELOG);
        log.info("Tenant schema migrations complete for schema: {}", schema);
    }

    private void runMigrations(final String schema, final String changelogPath) throws Exception {
        try (final Connection connection = dataSource.getConnection()) {
            try (final Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
                stmt.execute("SET search_path TO " + schema);
            }

            final Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            database.setDefaultSchemaName(schema);
            database.setLiquibaseSchemaName(schema);

            try (final Liquibase liquibase = new Liquibase(
                    changelogPath,
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }
}
