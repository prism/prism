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

package org.prism_mc.prism.paper.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import io.leangen.geantyref.TypeToken;
import java.nio.file.Path;
import net.kyori.moonshine.Moonshine;
import net.kyori.moonshine.exception.scan.UnscannableMethodException;
import net.kyori.moonshine.strategy.StandardPlaceholderResolverStrategy;
import net.kyori.moonshine.strategy.supertype.StandardSupertypeThenInterfaceSupertypeStrategy;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.prism_mc.prism.api.actions.types.ActionTypeRegistry;
import org.prism_mc.prism.api.activities.AbstractActivity;
import org.prism_mc.prism.api.services.modifications.ModificationQueueResult;
import org.prism_mc.prism.api.services.modifications.ModificationQueueService;
import org.prism_mc.prism.api.services.modifications.ModificationResult;
import org.prism_mc.prism.api.services.modifications.Restore;
import org.prism_mc.prism.api.services.modifications.Rollback;
import org.prism_mc.prism.api.services.purges.PurgeCycleResult;
import org.prism_mc.prism.api.services.purges.PurgeQueue;
import org.prism_mc.prism.api.services.recording.RecordingService;
import org.prism_mc.prism.api.services.wands.Wand;
import org.prism_mc.prism.api.services.wands.WandMode;
import org.prism_mc.prism.api.storage.StorageAdapter;
import org.prism_mc.prism.core.injection.factories.FileSqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.injection.factories.PurgeQueueFactory;
import org.prism_mc.prism.core.injection.factories.RestoreFactory;
import org.prism_mc.prism.core.injection.factories.RollbackFactory;
import org.prism_mc.prism.core.injection.factories.SqlActivityQueryBuilderFactory;
import org.prism_mc.prism.core.services.cache.CacheService;
import org.prism_mc.prism.core.storage.adapters.h2.H2StorageAdapter;
import org.prism_mc.prism.core.storage.adapters.mariadb.MariaDbStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.mysql.MysqlStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.postgres.PostgresStorageAdapter;
import org.prism_mc.prism.core.storage.adapters.sql.FileSqlActivityQueryBuilder;
import org.prism_mc.prism.core.storage.adapters.sql.SqlActivityQueryBuilder;
import org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdater;
import org.prism_mc.prism.core.storage.adapters.sqlite.SqliteStorageAdapter;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.loader.storage.StorageType;
import org.prism_mc.prism.paper.PrismPaper;
import org.prism_mc.prism.paper.actions.types.PaperActionTypeRegistry;
import org.prism_mc.prism.paper.integrations.worldedit.WorldEditIntegration;
import org.prism_mc.prism.paper.providers.TaskChainProvider;
import org.prism_mc.prism.paper.services.alerts.BlockBreakAlertData;
import org.prism_mc.prism.paper.services.alerts.ItemAlertData;
import org.prism_mc.prism.paper.services.alerts.PaperAlertService;
import org.prism_mc.prism.paper.services.expectations.ExpectationService;
import org.prism_mc.prism.paper.services.filters.PaperFilterService;
import org.prism_mc.prism.paper.services.lookup.LookupService;
import org.prism_mc.prism.paper.services.messages.MessageRenderer;
import org.prism_mc.prism.paper.services.messages.MessageSender;
import org.prism_mc.prism.paper.services.messages.MessageService;
import org.prism_mc.prism.paper.services.messages.ReceiverResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.ActivityPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.BlockBreakAlertDataPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.IntegerPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.ItemAlertDataPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.LongPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.ModificationQueueResultPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.ModificationResultPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.PaginationResultPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.PurgeCycleResultPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.StringPlaceholderResolver;
import org.prism_mc.prism.paper.services.messages.resolvers.WandModePlaceholderResolver;
import org.prism_mc.prism.paper.services.modifications.PaperModificationQueueService;
import org.prism_mc.prism.paper.services.modifications.PaperRestore;
import org.prism_mc.prism.paper.services.modifications.PaperRollback;
import org.prism_mc.prism.paper.services.nbt.NbtService;
import org.prism_mc.prism.paper.services.pagination.PaginationService;
import org.prism_mc.prism.paper.services.purge.PaperPurgeQueue;
import org.prism_mc.prism.paper.services.purge.PurgeService;
import org.prism_mc.prism.paper.services.query.QueryService;
import org.prism_mc.prism.paper.services.recording.PaperRecordingService;
import org.prism_mc.prism.paper.services.scheduling.SchedulingService;
import org.prism_mc.prism.paper.services.translation.PaperTranslationService;
import org.prism_mc.prism.paper.services.wands.InspectionWand;
import org.prism_mc.prism.paper.services.wands.RestoreWand;
import org.prism_mc.prism.paper.services.wands.RollbackWand;
import org.prism_mc.prism.paper.services.wands.WandService;

public class PrismModule extends AbstractModule {

    /**
     * The plugin.
     */
    private final PrismPaper prism;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The data path.
     */
    private final Path dataPath;

    /**
     * The version.
     */
    private final String version;

    /**
     * The serializer version.
     */
    private final short serializerVersion;

    /**
     * Construct the module.
     *
     * @param prism Prism
     * @param loggingService The logging service
     */
    public PrismModule(PrismPaper prism, LoggingService loggingService) {
        this.prism = prism;
        this.loggingService = loggingService;
        this.dataPath = prism.loaderPlugin().getDataFolder().toPath();
        this.version = prism.loaderPlugin().getDescription().getVersion();
        this.serializerVersion = prism.serializerVersion();
    }

    @Provides
    @Named("version")
    String getVersion() {
        return version;
    }

    @Provides
    @Named("serializerVersion")
    short serializerVersion() {
        return serializerVersion;
    }

    /**
     * Get the world edit integration.
     *
     * @param recordingService The recording service
     * @param configurationService The configuration service
     * @return The world edit integration
     */
    @Provides
    @Singleton
    @Inject
    public WorldEditIntegration getWorldEditIntegeration(
        PaperRecordingService recordingService,
        ConfigurationService configurationService
    ) {
        // Check for WorldEdit
        Plugin worldEdit = Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        if (worldEdit != null) {
            return new WorldEditIntegration(loggingService, worldEdit, recordingService, configurationService);
        }

        return null;
    }

    /**
     * Get the message service.
     *
     * @param translationService The translation service
     * @param messageRenderer The message renderer
     * @param messageSender The message sender
     * @param activityPlaceholderResolver The activity placeholder resolver
     * @param wandModePlaceholderResolver The wand mode resolver
     * @return The message service
     */
    @Provides
    @Singleton
    @Inject
    public MessageService getMessageService(
        PaperTranslationService translationService,
        MessageRenderer messageRenderer,
        MessageSender messageSender,
        ActivityPlaceholderResolver activityPlaceholderResolver,
        ModificationQueueResultPlaceholderResolver modificationQueueResultPlaceholderResolver,
        WandModePlaceholderResolver wandModePlaceholderResolver
    ) {
        try {
            return Moonshine.<MessageService, CommandSender>builder(TypeToken.get(MessageService.class))
                .receiverLocatorResolver(new ReceiverResolver(), 0)
                .sourced(translationService)
                .rendered(messageRenderer)
                .sent(messageSender)
                .resolvingWithStrategy(
                    new StandardPlaceholderResolverStrategy<>(
                        new StandardSupertypeThenInterfaceSupertypeStrategy(false)
                    )
                )
                .weightedPlaceholderResolver(Integer.class, new IntegerPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(Long.class, new LongPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(String.class, new StringPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(PurgeCycleResult.class, new PurgeCycleResultPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(AbstractActivity.class, activityPlaceholderResolver, 0)
                .weightedPlaceholderResolver(WandMode.class, wandModePlaceholderResolver, 0)
                .weightedPlaceholderResolver(
                    ModificationQueueResult.class,
                    modificationQueueResultPlaceholderResolver,
                    0
                )
                .weightedPlaceholderResolver(new TypeToken<>() {}, new PaginationResultPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(ItemAlertData.class, new ItemAlertDataPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(BlockBreakAlertData.class, new BlockBreakAlertDataPlaceholderResolver(), 0)
                .weightedPlaceholderResolver(ModificationResult.class, new ModificationResultPlaceholderResolver(), 0)
                .create(this.getClass().getClassLoader());
        } catch (UnscannableMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void configure() {
        // Base
        bind(Path.class).toInstance(dataPath);

        // Taskchain
        bind(TaskChainProvider.class).toInstance(new TaskChainProvider(prism.loader()));

        // Actions
        bind(ActionTypeRegistry.class).to(PaperActionTypeRegistry.class).in(Singleton.class);

        // Service - Alerts
        bind(PaperAlertService.class).in(Singleton.class);

        // Service - Cache
        bind(CacheService.class).in(Singleton.class);

        // Service - Configuration
        bind(ConfigurationService.class).toInstance(prism.loader().configurationService());

        // Service - Expectations
        bind(ExpectationService.class).in(Singleton.class);

        // Service - Filters
        bind(PaperFilterService.class).in(Singleton.class);

        // Service = Logging
        bind(LoggingService.class).toInstance(loggingService);

        // Service - Lookup
        bind(LookupService.class).in(Singleton.class);

        // Service - Messages
        bind(MessageRenderer.class).in(Singleton.class);
        bind(MessageSender.class).in(Singleton.class);
        bind(ActivityPlaceholderResolver.class).in(Singleton.class);

        // Service - Modifications
        bind(ModificationQueueService.class).to(PaperModificationQueueService.class).in(Singleton.class);

        // Service - Nbt
        bind(NbtService.class).in(Singleton.class);

        // Service - Pagination
        bind(PaginationService.class).in(Singleton.class);

        install(new FactoryModuleBuilder().implement(Restore.class, PaperRestore.class).build(RestoreFactory.class));

        install(new FactoryModuleBuilder().implement(Rollback.class, PaperRollback.class).build(RollbackFactory.class));

        // Service - Purge
        bind(PurgeService.class).in(Singleton.class);

        install(
            new FactoryModuleBuilder().implement(PurgeQueue.class, PaperPurgeQueue.class).build(PurgeQueueFactory.class)
        );

        // Service - Query
        bind(QueryService.class).in(Singleton.class);

        // Service - Recording
        bind(RecordingService.class).to(PaperRecordingService.class).in(Singleton.class);

        // Service - Scheduling
        bind(SchedulingService.class).in(Singleton.class);

        // Service - Translation
        bind(PaperTranslationService.class).in(Singleton.class);

        // Service - Wands
        bind(WandService.class).in(Singleton.class);
        MapBinder<WandMode, Wand> wandBinder = MapBinder.newMapBinder(binder(), WandMode.class, Wand.class);
        wandBinder.addBinding(WandMode.INSPECT).to(InspectionWand.class);
        wandBinder.addBinding(WandMode.ROLLBACK).to(RollbackWand.class);
        wandBinder.addBinding(WandMode.RESTORE).to(RestoreWand.class);

        // Storage
        bind(SqlSchemaUpdater.class).in(Singleton.class);

        StorageType storageType = prism.loader().configurationService().storageConfig().primaryStorageType();

        // Install the correct query builder
        if (storageType.equals(StorageType.SQLITE) || storageType.equals(StorageType.H2)) {
            install(
                new FactoryModuleBuilder()
                    .implement(SqlActivityQueryBuilder.class, FileSqlActivityQueryBuilder.class)
                    .build(FileSqlActivityQueryBuilderFactory.class)
            );
        } else {
            install(
                new FactoryModuleBuilder()
                    .implement(SqlActivityQueryBuilder.class, SqlActivityQueryBuilder.class)
                    .build(SqlActivityQueryBuilderFactory.class)
            );
        }

        // Bind the correct storage adapter
        switch (storageType) {
            case H2 -> bind(StorageAdapter.class).to(H2StorageAdapter.class).in(Singleton.class);
            case MARIADB -> bind(StorageAdapter.class).to(MariaDbStorageAdapter.class).in(Singleton.class);
            case MYSQL -> bind(StorageAdapter.class).to(MysqlStorageAdapter.class).in(Singleton.class);
            case POSTGRES -> bind(StorageAdapter.class).to(PostgresStorageAdapter.class).in(Singleton.class);
            case SQLITE -> bind(StorageAdapter.class).to(SqliteStorageAdapter.class).in(Singleton.class);
            default -> {
                // ignored
            }
        }
    }
}
