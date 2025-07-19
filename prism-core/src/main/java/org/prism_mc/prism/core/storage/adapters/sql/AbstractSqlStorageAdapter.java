/*
 * prism
 *
 * Copyright (c) 2022 M Botsko (viveleroi)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.prism_mc.prism.core.storage.adapters.sql;

import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.constraint;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.types.UShort;
import org.prism_mc.prism.api.actions.ActionData;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.activities.Activity;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.api.activities.Cause;
import org.prism_mc.prism.api.activities.GroupedActivity;
import org.prism_mc.prism.api.containers.PlayerContainer;
import org.prism_mc.prism.api.containers.StringContainer;
import org.prism_mc.prism.api.containers.TranslatableContainer;
import org.prism_mc.prism.api.services.pagination.PartialListPaginationResult;
import org.prism_mc.prism.api.storage.ActivityBatch;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.api.util.Coordinate;
import org.prism_mc.prism.api.util.Pair;
import org.prism_mc.prism.core.injection.factories.SqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.HikariConfigFactories;
import org.prism_mc.prism.core.storage.dbo.DefaultCatalog;
import org.prism_mc.prism.core.storage.dbo.Indexes;
import org.prism_mc.prism.core.storage.dbo.PrismDatabase;
import org.prism_mc.prism.core.storage.dbo.records.PrismActionsRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismEntityTypesRecord;
import org.prism_mc.prism.core.storage.dbo.records.PrismWorldsRecord;
import org.prism_mc.prism.core.storage.dbo.tables.PrismActions;
import org.prism_mc.prism.core.storage.dbo.tables.PrismActivities;
import org.prism_mc.prism.core.storage.dbo.tables.PrismBlocks;
import org.prism_mc.prism.core.storage.dbo.tables.PrismCauses;
import org.prism_mc.prism.core.storage.dbo.tables.PrismEntityTypes;
import org.prism_mc.prism.core.storage.dbo.tables.PrismItems;
import org.prism_mc.prism.core.storage.dbo.tables.PrismMeta;
import org.prism_mc.prism.core.storage.dbo.tables.PrismPlayers;
import org.prism_mc.prism.core.storage.dbo.tables.PrismWorlds;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;

public abstract class AbstractSqlStorageAdapter implements StorageAdapter {

    /**
     * The prism database object model.
     */
    public static PrismDatabase PRISM_DATABASE;

    /**
     * The actions dbo.
     */
    public static PrismActions PRISM_ACTIONS;

    /**
     * The activities dbo.
     */
    public static PrismActivities PRISM_ACTIVITIES;

    /**
     * The blocks dbo.
     */
    public static PrismBlocks PRISM_BLOCKS;

    /**
     * The causes dbo.
     */
    public static PrismCauses PRISM_CAUSES;

    /**
     * The entity types dbo.
     */
    public static PrismEntityTypes PRISM_ENTITY_TYPES;

    /**
     * The materials dbo.
     */
    public static PrismItems PRISM_ITEMS;

    /**
     * The meta dbo.
     */
    public static PrismMeta PRISM_META;

    /**
     * The players dbo.
     */
    public static PrismPlayers PRISM_PLAYERS;

    /**
     * The worlds dbo.
     */
    public static PrismWorlds PRISM_WORLDS;

    /**
     * The aliased replaced blocks table.
     */
    public static PrismBlocks REPLACED_BLOCKS;

    /**
     * The aliased affected players table.
     */
    public static PrismPlayers AFFECTED_PLAYERS;

    /**
     * The aliased cause entity types table.
     */
    public static PrismEntityTypes CAUSE_ENTITY_TYPES;

    /**
     * The aliased cause blocks table.
     */
    public static PrismBlocks CAUSE_BLOCKS;

    /**
     * The aliased replaced blocks translation key.
     */
    public static Field<String> REPLACED_BLOCKS_TRANSLATION_KEY;

    /**
     * The aliased cause blocks translation key.
     */
    public static Field<String> CAUSE_BLOCKS_TRANSLATION_KEY;

    /**
     * The aliased cause entity types translation key.
     */
    public static Field<String> CAUSE_ENTITY_TYPES_TRANSLATION_KEY;

    /**
     * The hikari config.
     */
    protected HikariConfig hikariConfig;

    /**
     * The hikari properties file.
     */
    protected File hikariPropertiesFile;

    /**
     * The serializer version.
     */
    protected final short serializerVersion;

    /**
     * The logging service.
     */
    protected final LoggingService loggingService;

    /**
     * The configuration service.
     */
    protected final ConfigurationService configurationService;

    /**
     * The action type registry.
     */
    protected final ActionTypeRegistry actionRegistry;

    /**
     * The schema updater.
     */
    protected final SqlSchemaUpdater schemaUpdater;

    /**
     * The query factory.
     */
    protected final SqlActivityQueryBuilderFactory queryBuilderFactory;

    /**
     * The query builder.
     */
    protected SqlActivityQueryBuilder queryBuilder;

    /**
     * The cache service.
     */
    protected final CacheService cacheService;

    /**
     * The hikari data source.
     */
    protected HikariDataSource dataSource;

    /**
     * The dsl context.
     */
    protected DSLContext dslContext;

    /**
     * The schema/table prefix.
     */
    protected String prefix;

    /**
     * Toggle whether this storage system is enabled and ready.
     */
    protected boolean ready = false;

    /**
     * Constructor.
     *
     * @param loggingService The logging service
     * @param configurationService The configuration service
     * @param actionRegistry The action type registry
     * @param schemaUpdater The schema updater
     * @param cacheService The cache service
     * @param queryBuilderFactory The query builder
     * @param serializerVersion The serializer version
     * @param dataPath The data path
     */
    @Inject
    public AbstractSqlStorageAdapter(
        LoggingService loggingService,
        ConfigurationService configurationService,
        ActionTypeRegistry actionRegistry,
        SqlSchemaUpdater schemaUpdater,
        SqlActivityQueryBuilderFactory queryBuilderFactory,
        CacheService cacheService,
        @Named("serializerVersion") short serializerVersion,
        Path dataPath
    ) {
        this.loggingService = loggingService;
        this.configurationService = configurationService;
        this.actionRegistry = actionRegistry;
        this.schemaUpdater = schemaUpdater;
        this.cacheService = cacheService;
        this.queryBuilderFactory = queryBuilderFactory;
        this.serializerVersion = serializerVersion;

        this.hikariPropertiesFile = new File(dataPath.toFile(), "hikari.properties");

        this.prefix = configurationService.storageConfig().primaryDataSource().prefix();
        loggingService.info(
            "Catalog {0}; Schema {1}; Prefix {2}",
            configurationService.storageConfig().primaryDataSource().catalog(),
            configurationService.storageConfig().primaryDataSource().schema(),
            prefix
        );

        var catalog = new DefaultCatalog(configurationService.storageConfig().primaryDataSource().catalog());

        // Initialize all of our DBOs
        PRISM_ACTIONS = new PrismActions(prefix);
        PRISM_ACTIVITIES = new PrismActivities(prefix);
        PRISM_BLOCKS = new PrismBlocks(prefix);
        PRISM_CAUSES = new PrismCauses(prefix);
        PRISM_ENTITY_TYPES = new PrismEntityTypes(prefix);
        PRISM_ITEMS = new PrismItems(prefix);
        PRISM_META = new PrismMeta(prefix);
        PRISM_PLAYERS = new PrismPlayers(prefix);
        PRISM_WORLDS = new PrismWorlds(prefix);
        PRISM_DATABASE = new PrismDatabase(
            catalog,
            configurationService.storageConfig().primaryDataSource().schema(),
            Arrays.asList(
                PRISM_ACTIONS,
                PRISM_ACTIVITIES,
                PRISM_BLOCKS,
                PRISM_CAUSES,
                PRISM_ENTITY_TYPES,
                PRISM_ITEMS,
                PRISM_META,
                PRISM_PLAYERS,
                PRISM_WORLDS
            )
        );

        // Table aliases
        REPLACED_BLOCKS = PRISM_BLOCKS.as("replaced_blocks");
        AFFECTED_PLAYERS = PRISM_PLAYERS.as("affected_players");
        CAUSE_BLOCKS = PRISM_BLOCKS.as("cause_blocks");
        CAUSE_ENTITY_TYPES = PRISM_ENTITY_TYPES.as("cause_entity_types");

        /*
         * Field aliases
         * For some reason JOOQ throws ambiguous field errors even though the tables are aliased
         * and the query is fine, so we alias these fields too.
         */
        REPLACED_BLOCKS_TRANSLATION_KEY = REPLACED_BLOCKS.TRANSLATION_KEY.as("replaced_block_translation_key");
        CAUSE_ENTITY_TYPES_TRANSLATION_KEY = CAUSE_ENTITY_TYPES.TRANSLATION_KEY.as("cause_entity_type_translation_key");
        CAUSE_BLOCKS_TRANSLATION_KEY = CAUSE_BLOCKS.TRANSLATION_KEY.as("cause_block_translation_key");

        // Turn off jooq crap. Lame
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        // Load any drivers
        HikariConfigFactories.loadDriver(configurationService.storageConfig().primaryStorageType());
        listDrivers();
    }

    /**
     * Connect to the data source.
     *
     * @param hikariConfig The hikari config
     */
    protected boolean connect(HikariConfig hikariConfig, SQLDialect sqlDialect) {
        this.hikariConfig = hikariConfig;

        loggingService.info("Connecting to {0}", hikariConfig.getJdbcUrl());

        try {
            dataSource = new HikariDataSource(hikariConfig);
            dslContext = DSL.using(dataSource, sqlDialect);

            if (queryBuilderFactory != null) {
                this.queryBuilder = queryBuilderFactory.create(dslContext);
            }

            return true;
        } catch (Exception e) {
            String msg =
                "Failed to connect to your database server. Please check:\n" +
                "- the ip/address\n" +
                "- the port\n" +
                "- your username/password\n" +
                "- any firewall rules\n" +
                "- that the database server is running\n";
            loggingService.warn(msg);
        }

        return false;
    }

    /**
     * List all drivers available in the driver manager.
     */
    protected void listDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        if (!drivers.hasMoreElements()) {
            loggingService.info("No database drivers detected!");
        }
        while (drivers.hasMoreElements()) {
            loggingService.info("Database driver: {0}", drivers.nextElement().getClass());
        }
    }

    /**
     * Detect version and other information regarding the database.
     *
     * @param hikariConfig The hikari config
     * @param usingHikariProperties Whether using hikari properties
     * @throws SQLException The database exception
     */
    protected void describeDatabase(HikariConfig hikariConfig, boolean usingHikariProperties) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            String databaseProduct = databaseMetaData.getDatabaseProductName();
            String databaseVersion = databaseMetaData.getDatabaseProductVersion();

            loggingService.info("Database: {0} {1}", databaseProduct, databaseVersion);
        }
    }

    /**
     * Create tables.
     *
     * @throws SQLException The database exception
     */
    protected void prepareSchema() throws Exception {
        // Create the metadata table
        dslContext
            .createTableIfNotExists(PRISM_META)
            .column(PRISM_META.META_ID)
            .column(PRISM_META.K)
            .column(PRISM_META.V)
            .primaryKey(PRISM_META.META_ID)
            .unique(PRISM_META.K)
            .execute();

        // Query for an existing schema version
        String schemaVersion = dslContext
            .select(PRISM_META.V)
            .from(PRISM_META)
            .where(PRISM_META.K.eq("schema_ver"))
            .fetchOne(PRISM_META.V);

        if (schemaVersion != null) {
            loggingService.info("Prism schema version: {0}", schemaVersion);

            updateSchemas(schemaVersion);
        } else {
            // Insert the schema version
            dslContext.insertInto(PRISM_META, PRISM_META.K, PRISM_META.V).values("schema_ver", "400").execute();
        }

        // Create the players table
        dslContext
            .createTableIfNotExists(PRISM_PLAYERS)
            .column(PRISM_PLAYERS.PLAYER_ID)
            .column(PRISM_PLAYERS.PLAYER)
            .column(PRISM_PLAYERS.PLAYER_UUID)
            .primaryKey(PRISM_PLAYERS.PLAYER_ID)
            .unique(PRISM_PLAYERS.PLAYER_UUID)
            .execute();

        // Create the blocks table
        dslContext
            .createTableIfNotExists(PRISM_BLOCKS)
            .column(PRISM_BLOCKS.BLOCK_ID)
            .column(PRISM_BLOCKS.NS)
            .column(PRISM_BLOCKS.NAME)
            .column(PRISM_BLOCKS.DATA)
            .column(PRISM_BLOCKS.TRANSLATION_KEY)
            .primaryKey(PRISM_BLOCKS.BLOCK_ID)
            .unique(PRISM_BLOCKS.NS, PRISM_BLOCKS.NAME, PRISM_BLOCKS.DATA)
            .execute();

        // Create the entity types table
        dslContext
            .createTableIfNotExists(PRISM_ENTITY_TYPES)
            .column(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
            .column(PRISM_ENTITY_TYPES.ENTITY_TYPE)
            .column(PRISM_ENTITY_TYPES.TRANSLATION_KEY)
            .primaryKey(PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
            .unique(PRISM_ENTITY_TYPES.ENTITY_TYPE)
            .execute();

        // Create the causes table
        dslContext
            .createTableIfNotExists(PRISM_CAUSES)
            .column(PRISM_CAUSES.CAUSE_ID)
            .column(PRISM_CAUSES.CAUSE)
            .primaryKey(PRISM_CAUSES.CAUSE_ID)
            .unique(PRISM_CAUSES.CAUSE)
            .execute();

        // Create actions table
        dslContext
            .createTableIfNotExists(PRISM_ACTIONS)
            .column(PRISM_ACTIONS.ACTION_ID)
            .column(PRISM_ACTIONS.ACTION)
            .primaryKey(PRISM_ACTIONS.ACTION_ID)
            .unique(PRISM_ACTIONS.ACTION)
            .execute();

        // Create the item table
        dslContext
            .createTableIfNotExists(PRISM_ITEMS)
            .column(PRISM_ITEMS.ITEM_ID)
            .column(PRISM_ITEMS.MATERIAL)
            .column(PRISM_ITEMS.DATA)
            .primaryKey(PRISM_ITEMS.ITEM_ID)
            .execute();

        // Create the worlds table
        dslContext
            .createTableIfNotExists(PRISM_WORLDS)
            .column(PRISM_WORLDS.WORLD_ID)
            .column(PRISM_WORLDS.WORLD)
            .column(PRISM_WORLDS.WORLD_UUID)
            .primaryKey(PRISM_WORLDS.WORLD_ID)
            .unique(PRISM_WORLDS.WORLD_UUID)
            .execute();

        // Create the activities table. This one's the fatso.
        dslContext
            .createTableIfNotExists(PRISM_ACTIVITIES)
            .column(PRISM_ACTIVITIES.ACTIVITY_ID)
            .column(PRISM_ACTIVITIES.TIMESTAMP)
            .column(PRISM_ACTIVITIES.WORLD_ID)
            .column(PRISM_ACTIVITIES.X)
            .column(PRISM_ACTIVITIES.Y)
            .column(PRISM_ACTIVITIES.Z)
            .column(PRISM_ACTIVITIES.ACTION_ID)
            .column(PRISM_ACTIVITIES.AFFECTED_ITEM_ID)
            .column(PRISM_ACTIVITIES.AFFECTED_ITEM_QUANTITY)
            .column(PRISM_ACTIVITIES.AFFECTED_BLOCK_ID)
            .column(PRISM_ACTIVITIES.REPLACED_BLOCK_ID)
            .column(PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID)
            .column(PRISM_ACTIVITIES.AFFECTED_PLAYER_ID)
            .column(PRISM_ACTIVITIES.CAUSE_ID)
            .column(PRISM_ACTIVITIES.CAUSE_PLAYER_ID)
            .column(PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID)
            .column(PRISM_ACTIVITIES.CAUSE_BLOCK_ID)
            .column(PRISM_ACTIVITIES.DESCRIPTOR)
            .column(PRISM_ACTIVITIES.METADATA)
            .column(PRISM_ACTIVITIES.SERIALIZER_VERSION)
            .column(PRISM_ACTIVITIES.SERIALIZED_DATA)
            .column(PRISM_ACTIVITIES.REVERSED)
            .primaryKey(PRISM_ACTIVITIES.ACTIVITY_ID)
            .constraints(
                constraint(String.format("%s_actionId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.ACTION_ID)
                    .references(PRISM_ACTIONS, PRISM_ACTIONS.ACTION_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_affectedEntityTypeId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID)
                    .references(PRISM_ENTITY_TYPES, PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_affectedItemId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.AFFECTED_ITEM_ID)
                    .references(PRISM_ITEMS, PRISM_ITEMS.ITEM_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_affectedBlockId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.AFFECTED_BLOCK_ID)
                    .references(PRISM_BLOCKS, PRISM_BLOCKS.BLOCK_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_replacedBlockId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.REPLACED_BLOCK_ID)
                    .references(PRISM_BLOCKS, PRISM_BLOCKS.BLOCK_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_affectedPlayerId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.AFFECTED_PLAYER_ID)
                    .references(PRISM_PLAYERS, PRISM_PLAYERS.PLAYER_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_causeId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.CAUSE_ID)
                    .references(PRISM_CAUSES, PRISM_CAUSES.CAUSE_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_causePlayerId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.CAUSE_PLAYER_ID)
                    .references(PRISM_PLAYERS, PRISM_PLAYERS.PLAYER_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_causeEntityTypeId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID)
                    .references(PRISM_ENTITY_TYPES, PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_causeBlockId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.CAUSE_BLOCK_ID)
                    .references(PRISM_BLOCKS, PRISM_BLOCKS.BLOCK_ID)
                    .onDeleteCascade(),
                constraint(String.format("%s_worldId", prefix))
                    .foreignKey(PRISM_ACTIVITIES.WORLD_ID)
                    .references(PRISM_WORLDS, PRISM_WORLDS.WORLD_ID)
                    .onDeleteCascade()
            )
            .execute();

        // Sqlite doesn't support creating indexes inline with create table and IF NOT EXISTS isn't a thing for indexes
        var indexNames = dslContext
            .meta()
            .getIndexes()
            .stream()
            .map(org.jooq.Named::getName)
            .collect(Collectors.toCollection(ArrayList::new));

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_ACTION_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_ACTION_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.ACTION_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_AFFECTED_ENTITY_TYPE_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_AFFECTED_ENTITY_TYPE_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.AFFECTED_ENTITY_TYPE_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_AFFECTED_ITEM_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_AFFECTED_ITEM_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.AFFECTED_ITEM_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_AFFECTED_BLOCK_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_AFFECTED_BLOCK_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.AFFECTED_BLOCK_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_REPLACED_BLOCK_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.AFFECTED_BLOCK_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_AFFECTED_PLAYER_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_AFFECTED_PLAYER_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.AFFECTED_PLAYER_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_CAUSE_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_CAUSE_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.CAUSE_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_CAUSE_PLAYER_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_CAUSE_PLAYER_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.CAUSE_PLAYER_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_CAUSE_ENTITY_TYPE_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_CAUSE_ENTITY_TYPE_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.CAUSE_ENTITY_TYPE_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_CAUSE_BLOCK_ID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_CAUSE_BLOCK_ID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.CAUSE_BLOCK_ID)
                .execute();
        }

        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_WORLDID.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_WORLDID)
                .on(PRISM_ACTIVITIES, PRISM_ACTIVITIES.WORLD_ID)
                .execute();
        }

        // Create a composite index for world, coordinate, and timestamp since most lookups use all three
        if (!indexNames.contains(Indexes.PRISM_ACTIVITIES_COORDINATE.getName())) {
            dslContext
                .createIndex(Indexes.PRISM_ACTIVITIES_COORDINATE)
                .on(
                    PRISM_ACTIVITIES,
                    PRISM_ACTIVITIES.WORLD_ID,
                    PRISM_ACTIVITIES.X,
                    PRISM_ACTIVITIES.Y,
                    PRISM_ACTIVITIES.Z,
                    PRISM_ACTIVITIES.TIMESTAMP
                )
                .execute();
        }
    }

    /**
     * Caching often-used object->primary-key lookups greatly reduce the number of queries/network requests.
     *
     * <p>Note: Player UUIDs are cached as needed and removed on disconnect.</p>
     */
    protected void prepareCache() {
        // Actions
        List<PrismActionsRecord> actions = dslContext
            .select(PRISM_ACTIONS.ACTION, PRISM_ACTIONS.ACTION_ID)
            .from(PRISM_ACTIONS)
            .fetchInto(PrismActionsRecord.class);

        for (PrismActionsRecord actionsRecord : actions) {
            int actionId = actionsRecord.getActionId().intValue();
            cacheService.actionKeyPkMap().put(actionsRecord.getAction(), actionId);
        }

        // Entity Types
        List<PrismEntityTypesRecord> entityTypes = dslContext
            .select(PRISM_ENTITY_TYPES.ENTITY_TYPE, PRISM_ENTITY_TYPES.ENTITY_TYPE_ID)
            .from(PRISM_ENTITY_TYPES)
            .fetchInto(PrismEntityTypesRecord.class);

        for (PrismEntityTypesRecord entityTypesRecord : entityTypes) {
            int entityTypeId = entityTypesRecord.getEntityTypeId().intValue();
            cacheService.entityTypePkMap().put(entityTypesRecord.getEntityType(), entityTypeId);
        }

        // World
        List<PrismWorldsRecord> worlds = dslContext
            .select(PRISM_WORLDS.WORLD_UUID, PRISM_WORLDS.WORLD_ID)
            .from(PRISM_WORLDS)
            .fetchInto(PrismWorldsRecord.class);

        for (PrismWorldsRecord worldsRecord : worlds) {
            int worldId = worldsRecord.getWorldId().intValue();
            UUID worldUuid = UUID.fromString(worldsRecord.getWorldUuid());
            cacheService.worldUuidPkMap().put(worldUuid, worldId);
        }
    }

    /**
     * Update the schema as needed.
     *
     * @throws SQLException The database exception
     */
    protected void updateSchemas(String schemaVersion) throws Exception {}

    @Override
    public List<Activity> queryActivities(ActivityQuery query) throws Exception {
        var results = activityMapper(queryBuilder.queryActivities(query), query);

        List<Activity> activities = new ArrayList<>();
        for (var result : results) {
            if (result instanceof Activity activity) {
                activities.add(activity);
            }
        }

        return activities;
    }

    @Override
    public PartialListPaginationResult<AbstractActivity> queryActivitiesPaginated(ActivityQuery query) {
        Result<org.jooq.Record> result = queryBuilder.queryActivities(query);

        int totalResults = result.size();
        if (!result.isEmpty()) {
            totalResults = result.get(0).getValue("totalrows", Integer.class);
        }

        int currentPage = (query.offset() / query.limit()) + 1;

        return new PartialListPaginationResult<>(
            activityMapper(result, query),
            totalResults,
            query.limit(),
            currentPage
        );
    }

    /**
     * Maps activity data to an action and activity record.
     *
     * @param result The result
     * @param query The original query
     * @return The activity list
     */
    protected List<AbstractActivity> activityMapper(Result<org.jooq.Record> result, ActivityQuery query) {
        List<AbstractActivity> activities = new ArrayList<>();

        for (org.jooq.Record r : result) {
            String actionKey = r.getValue(PRISM_ACTIONS.ACTION);
            var optionalActionType = actionRegistry.actionType(actionKey);
            if (optionalActionType.isEmpty()) {
                loggingService.warn("Failed to find action type: {0}", actionKey);
                continue;
            }

            var actionType = optionalActionType.get();

            // World
            UUID worldUuid = UUID.fromString(r.getValue(PRISM_WORLDS.WORLD_UUID));
            var world = new Pair<>(worldUuid, r.getValue(PRISM_WORLDS.WORLD));

            // Location
            int x = 0;
            int y = 0;
            int z = 0;
            if (query.grouped()) {
                x = r.getValue(avg(PRISM_ACTIVITIES.X)).intValue();
                y = r.getValue(avg(PRISM_ACTIVITIES.Y)).intValue();
                z = r.getValue(avg(PRISM_ACTIVITIES.Z)).intValue();
            } else {
                x = r.getValue(PRISM_ACTIVITIES.X);
                y = r.getValue(PRISM_ACTIVITIES.Y);
                z = r.getValue(PRISM_ACTIVITIES.Z);
            }

            var coordinate = new Coordinate(x, y, z);

            // Entity type
            String entityType = null;
            String entityTypeName = r.getValue(PRISM_ENTITY_TYPES.ENTITY_TYPE);
            if (entityTypeName != null) {
                entityType = entityTypeName.toUpperCase(Locale.ENGLISH);
            }

            // Material
            String material = null;
            String materialName = r.getValue(PRISM_ITEMS.MATERIAL);
            if (materialName != null) {
                material = materialName.toUpperCase(Locale.ENGLISH);
            }

            String itemData = r.getValue(PRISM_ITEMS.DATA);

            // Item quantity
            short itemQuantity = r.getValue(coalesce(PRISM_ACTIVITIES.AFFECTED_ITEM_QUANTITY, DSL.val(0))).shortValue();

            // Affected player
            String affectedPlayerName = r.getValue(AFFECTED_PLAYERS.PLAYER);
            UUID affectedPlayerUuid = null;
            if (r.getValue(AFFECTED_PLAYERS.PLAYER_UUID) != null) {
                affectedPlayerUuid = UUID.fromString(r.getValue(AFFECTED_PLAYERS.PLAYER_UUID));
            }

            // Cause
            Cause cause = null;
            if (query.lookup() && r.getValue(PRISM_CAUSES.CAUSE) != null) {
                cause = new Cause(new StringContainer(r.getValue(PRISM_CAUSES.CAUSE)));
            } else if (r.getValue(PRISM_PLAYERS.PLAYER_UUID) != null) {
                String playerName = r.getValue(PRISM_PLAYERS.PLAYER);
                UUID playerUuid = UUID.fromString(r.getValue(PRISM_PLAYERS.PLAYER_UUID));

                cause = new Cause(new PlayerContainer(playerName, playerUuid));
            } else if (query.lookup() && r.getValue(CAUSE_ENTITY_TYPES_TRANSLATION_KEY) != null) {
                cause = new Cause(new TranslatableContainer(r.getValue(CAUSE_ENTITY_TYPES_TRANSLATION_KEY)));
            } else if (query.lookup() && r.getValue(CAUSE_BLOCKS_TRANSLATION_KEY) != null) {
                cause = new Cause(new TranslatableContainer(r.getValue(CAUSE_BLOCKS_TRANSLATION_KEY)));
            }

            String descriptor = query.lookup() ? r.getValue(PRISM_ACTIVITIES.DESCRIPTOR) : null;
            String metadata = query.lookup() ? r.getValue(PRISM_ACTIVITIES.METADATA) : null;

            String blockNamespace = r.getValue(PRISM_BLOCKS.NS);
            String blockName = r.getValue(PRISM_BLOCKS.NAME);

            long timestamp;
            if (query.grouped()) {
                timestamp = r.getValue(avg(PRISM_ACTIVITIES.TIMESTAMP)).longValue();
            } else {
                timestamp = r.getValue(PRISM_ACTIVITIES.TIMESTAMP).longValue();
            }

            String translationKey = r.getValue(PRISM_BLOCKS.TRANSLATION_KEY);

            if (!query.grouped() && query.modification()) {
                long activityId = r.getValue(PRISM_ACTIVITIES.ACTIVITY_ID).longValue();

                String customData = r.getValue(PRISM_ACTIVITIES.SERIALIZED_DATA);
                UShort customDataVersion = r.getValue(PRISM_ACTIVITIES.SERIALIZER_VERSION);
                String blockData = r.getValue(PRISM_BLOCKS.DATA);
                String replacedBlockNamespace = r.getValue(REPLACED_BLOCKS.NS);
                String replacedBlockName = r.getValue(REPLACED_BLOCKS.NAME);
                String replacedBlockData = r.getValue(REPLACED_BLOCKS.DATA);
                String replacedBlockTranslationKey = r.getValue(REPLACED_BLOCKS_TRANSLATION_KEY);

                // Build the action data
                ActionData actionData = new ActionData(
                    material,
                    itemQuantity,
                    itemData,
                    blockNamespace,
                    blockName,
                    blockData,
                    replacedBlockNamespace,
                    replacedBlockName,
                    replacedBlockData,
                    entityType,
                    customData,
                    descriptor,
                    metadata,
                    customDataVersion.shortValue(),
                    translationKey,
                    replacedBlockTranslationKey,
                    null,
                    null
                );

                // Build the activity
                try {
                    var activity = new Activity(
                        activityId,
                        actionType.createAction(actionData),
                        world,
                        coordinate,
                        cause,
                        timestamp
                    );

                    // Add to result list
                    activities.add(activity);
                } catch (Exception e) {
                    loggingService.handleException(e);
                }
            } else if (!query.grouped()) {
                long activityId = r.getValue(PRISM_ACTIVITIES.ACTIVITY_ID).longValue();

                // Build the action data
                ActionData actionData = new ActionData(
                    material,
                    itemQuantity,
                    itemData,
                    blockNamespace,
                    blockName,
                    null,
                    null,
                    null,
                    null,
                    entityType,
                    null,
                    descriptor,
                    metadata,
                    (short) 0,
                    translationKey,
                    null,
                    affectedPlayerName,
                    affectedPlayerUuid
                );

                // Build the activity
                try {
                    var activity = new Activity(
                        activityId,
                        actionType.createAction(actionData),
                        world,
                        coordinate,
                        cause,
                        timestamp
                    );

                    // Add to result list
                    activities.add(activity);
                } catch (Exception e) {
                    loggingService.handleException(e);
                }
            } else {
                // Build the action data
                ActionData actionData = new ActionData(
                    material,
                    itemQuantity,
                    itemData,
                    blockNamespace,
                    blockName,
                    null,
                    null,
                    null,
                    null,
                    entityType,
                    null,
                    descriptor,
                    metadata,
                    (short) 0,
                    translationKey,
                    null,
                    affectedPlayerName,
                    affectedPlayerUuid
                );

                // Count
                int count = r.getValue("groupcount", Integer.class);

                // Build the grouped activity
                try {
                    var activity = new GroupedActivity(
                        actionType.createAction(actionData),
                        world,
                        coordinate,
                        cause,
                        timestamp,
                        count
                    );

                    // Add to result list
                    activities.add(activity);
                } catch (Exception e) {
                    loggingService.handleException(e);
                }
            }
        }

        return activities;
    }

    @Override
    public ActivityBatch createActivityBatch() {
        return new SqlActivityBatch(loggingService, dslContext, serializerVersion, cacheService);
    }

    @Override
    public int deleteActivities(ActivityQuery query, int cycleMinPrimaryKey, int cycleMaxPrimaryKey) {
        return queryBuilder.deleteActivities(query, cycleMinPrimaryKey, cycleMaxPrimaryKey);
    }

    @Override
    public Pair<Integer, Integer> getActivitiesPkBounds(ActivityQuery query) {
        return queryBuilder.queryActivitiesPkBounds(query);
    }

    @Override
    public void markReversed(List<Long> activityIds, boolean reversed) {
        if (activityIds.isEmpty()) {
            return;
        }

        dslContext
            .update(PRISM_ACTIVITIES)
            .set(PRISM_ACTIVITIES.REVERSED, reversed)
            .where(PRISM_ACTIVITIES.ACTIVITY_ID.in(activityIds))
            .execute();
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public void writeHikariPropertiesFile() throws IOException {
        Properties props = new Properties();

        if (hikariConfig.getDataSourceClassName() != null) {
            props.setProperty("dataSourceClassName", hikariConfig.getDataSourceClassName());
        }

        if (hikariConfig.getDriverClassName() != null) {
            props.setProperty("driverClassName", hikariConfig.getDriverClassName());
        }

        if (hikariConfig.getJdbcUrl() != null) {
            props.setProperty("jdbcUrl", hikariConfig.getJdbcUrl());
        }

        if (hikariConfig.getUsername() != null) {
            props.setProperty("username", hikariConfig.getUsername());
        }

        if (hikariConfig.getPassword() != null) {
            props.setProperty("password", hikariConfig.getPassword());
        }

        if (hikariConfig.getTransactionIsolation() != null) {
            props.setProperty("transactionIsolation", hikariConfig.getTransactionIsolation());
        }

        props.setProperty("maximumPoolSize", String.valueOf(hikariConfig.getMaximumPoolSize()));
        props.setProperty("poolName", hikariConfig.getPoolName());

        for (var entry : hikariConfig.getDataSourceProperties().entrySet()) {
            props.setProperty("dataSource." + entry.getKey().toString(), entry.getValue().toString());
        }

        try (FileOutputStream output = new FileOutputStream(hikariPropertiesFile)) {
            props.store(output, "HikariCP Database Configuration");
        }
    }

    /**
     * Loads sql files from the resource folder and replaces the prefix placeholder.
     *
     * @param storageType The storage type
     * @param sqlFileName The file for the needed sql
     * @param prefix The schema/table prefix
     * @return Sql statement string
     * @throws IOException File exception
     */
    protected String loadSqlFromResourceFile(String storageType, String sqlFileName, String prefix) throws IOException {
        var sql = loadResourceFileAsString(String.format("sql/%s/%s.sql", storageType, sqlFileName));
        return sql.replaceAll("%prefix%", prefix);
    }

    /**
     * Loads a file from the resource path.
     *
     * @param filePath The file path
     * @return The file contents as a string
     * @throws IOException File exception
     */
    private String loadResourceFileAsString(String filePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(filePath);

        if (inputStream == null) {
            throw new IOException("Could not find resource file: " + filePath);
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine()).append("\n");
            }
        }
        return stringBuilder.toString();
    }
}
