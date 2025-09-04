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

package org.prism_mc.prism.paper.services.scheduling;

import static org.quartz.JobBuilder.newJob;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.configuration.purge.CommandScheduleConfiguration;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

@Singleton
public class SchedulingService {

    /**
     * The cron parser.
     */
    private final CronParser cronParser;

    /**
     * The logging service.
     */
    private final LoggingService loggingService;

    /**
     * The scheduler.
     */
    private Scheduler scheduler = null;

    /**
     * Constructor.
     */
    @Inject
    public SchedulingService(ConfigurationService configurationService, LoggingService loggingService) {
        this.loggingService = loggingService;

        CronDefinition definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ);
        cronParser = new CronParser(definition);

        Properties quartzProperties = new Properties();
        quartzProperties.put("org.quartz.scheduler.instanceName", "PrismQuartzScheduler");
        quartzProperties.put("org.quartz.threadPool.threadCount", "2");

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory(quartzProperties);
            scheduler = schedulerFactory.getScheduler();

            for (var commandScheduleConfig : configurationService.prismConfig().purges().commandSchedules()) {
                if (commandScheduleConfig.enabled()) {
                    scheduleJob(commandScheduleConfig);
                }
            }

            scheduler.start();
        } catch (SchedulerException e) {
            loggingService.handleException(e);
        }
    }

    /**
     * Schedule a job.
     *
     * @param commandConfig The command configuration
     */
    private void scheduleJob(CommandScheduleConfiguration commandConfig) {
        if (scheduler == null) {
            loggingService.warn("Failed to schedule job because the scheduler was not initialized.");

            return;
        }

        String jobKey = UUID.randomUUID().toString();
        ZonedDateTime now = ZonedDateTime.now();

        // Verify the schedule start date is in the future
        Cron starts = cronParser.parse(commandConfig.cron());
        Optional<ZonedDateTime> startExec = ExecutionTime.forCron(starts).nextExecution(now);
        if (startExec.isPresent()) {
            try {
                // Build the start job detail
                JobDetail commandJob = newJob(PrismCommandJob.class)
                    .withIdentity("commandExec" + jobKey, "quartzGroup")
                    .usingJobData("command", commandConfig.command())
                    .build();

                // Build the start cron trigger
                CronTrigger commandTrigger = TriggerBuilder.newTrigger()
                    .withIdentity("commandCron" + jobKey, "quartzGroup")
                    .withSchedule(CronScheduleBuilder.cronSchedule(commandConfig.cron()))
                    .forJob("commandExec" + jobKey, "quartzGroup")
                    .build();

                scheduler.scheduleJob(commandJob, commandTrigger);
            } catch (SchedulerException e) {
                loggingService.handleException(e);
            }
        } else {
            loggingService.warn(
                "Skipping command due to an execution time with no future executions: {0}",
                commandConfig.command()
            );
        }
    }
}
