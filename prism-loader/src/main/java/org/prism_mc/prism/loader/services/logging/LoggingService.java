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

package org.prism_mc.prism.loader.services.logging;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;

public class LoggingService {

    /**
     * The configuration service.
     */
    private final ConfigurationService configurationService;

    /**
     * The logger.
     */
    private final Logger logger;

    /**
     * Constructor.
     *
     * @param configurationService The configuration service
     * @param logger The logger
     */
    public LoggingService(ConfigurationService configurationService, Logger logger) {
        this.configurationService = configurationService;
        this.logger = logger;
    }

    /**
     * Log a debug message.
     *
     * @param message The message
     */
    public void debug(String message) {
        if (configurationService.prismConfig().debug()) {
            logger.info(message);
        }
    }

    /**
     * Log the debug message and format args.
     *
     * @param message The string message
     * @param args The args
     */
    public void debug(String message, Object... args) {
        if (configurationService.prismConfig().debug()) {
            logger.log(Level.INFO, message, args);
        }
    }

    /**
     * Log an error message.
     *
     * @param message The string message
     */
    public void error(String message) {
        logger.log(Level.SEVERE, message);
    }

    /**
     * Log an error message and format args.
     *
     * @param message The string message
     * @param args The args
     */
    public void error(String message, Object... args) {
        logger.log(Level.SEVERE, message, args);
    }

    /**
     * Handle thrown errors/exceptions.
     *
     * @param message The message
     * @param throwable The thrown error/exception
     */
    public void handleThrowable(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    /**
     * Handle exceptions.
     *
     * @param exception The exception
     */
    public void handleException(Exception exception) {
        logger.log(Level.SEVERE, "An exception occurred", exception);
    }

    /**
     * Log a message.
     *
     * @param message The message
     */
    public void info(String message) {
        logger.info(message);
    }

    /**
     * Log a message and format args.
     *
     * @param message The string message
     * @param args The args
     */
    public void info(String message, Object... args) {
        logger.log(Level.INFO, message, args);
    }

    /**
     * Log a warning message.
     *
     * @param message The string message
     */
    public void warn(String message) {
        logger.log(Level.WARNING, message);
    }

    /**
     * Log a warning message and format args.
     *
     * @param message The string message
     * @param args The args
     */
    public void warn(String message, Object... args) {
        logger.log(Level.WARNING, message, args);
    }
}
