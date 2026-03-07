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

package org.prism_mc.prism.paper.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;
import lombok.experimental.UtilityClass;
import org.prism_mc.prism.loader.services.configuration.ConfigurationService;
import org.prism_mc.prism.loader.services.dependencies.loader.JarInJarClassLoader;
import org.prism_mc.prism.loader.services.logging.LoggingService;
import org.prism_mc.prism.loader.services.schema.SchemaUpdateCli;

/**
 * CLI entry point for running Prism utilities without a Minecraft server.
 */
@UtilityClass
public final class PrismCli {

    /**
     * The file name of the "JarInJar".
     */
    private static final String JAR_NAME = "prism-paper.jarinjar";

    /**
     * The qualified name of the schema update CLI runner.
     */
    private static final String SCHEMA_UPDATE_CLASS = "org.prism_mc.prism.core.storage.adapters.sql.SqlSchemaUpdateCli";

    /**
     * Main entry point.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return;
        }

        String command = args[0];
        if ("schema-update".equals(command)) {
            Path dataPath = parseConfigPath(args);
            runSchemaUpdate(dataPath);
        } else {
            System.err.println("Unknown command: " + command);
            printUsage();
            System.exit(1);
        }
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Prism CLI");
        System.out.println();
        System.out.println("Usage: java -jar prism-paper-<version>.jar <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  schema-update    Update the database schema to the latest version");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --config-path <path>    Path to the Prism config directory");
        System.out.println("                        (default: ./prism)");
    }

    /**
     * Parse the --config-path option from args, defaulting to ./prism.
     *
     * @param args The command line arguments
     * @return The config path
     */
    private static Path parseConfigPath(String[] args) {
        for (int i = 1; i < args.length - 1; i++) {
            if ("--config-path".equals(args[i])) {
                return Path.of(args[i + 1]);
            }
        }
        return Path.of("prism");
    }

    /**
     * Run the schema update.
     *
     * @param dataPath The data path
     */
    private static void runSchemaUpdate(Path dataPath) {
        Path resolvedPath = dataPath.toAbsolutePath().normalize();

        // Check if the Minecraft server is running
        checkServerNotRunning(resolvedPath);

        // Validate that storage.conf exists
        File storageConf = resolvedPath.resolve("storage.conf").toFile();
        if (!storageConf.exists()) {
            System.err.println("Error: storage.conf not found at " + resolvedPath);
            System.err.println("Ensure --config-path points to your Prism config directory " + "(e.g. plugins/prism)");
            System.exit(1);
        }

        Logger logger = Logger.getLogger("Prism");

        ConfigurationService configService = new ConfigurationService(resolvedPath, logger);
        LoggingService loggingService = new LoggingService(configService, logger);

        // Load the inner jar to access storage classes
        JarInJarClassLoader loader = new JarInJarClassLoader(PrismCli.class.getClassLoader(), JAR_NAME);

        // Load cached dependencies from the libs directory.
        // Dependencies are downloaded on first server start and cached here.
        Path libsDir = resolvedPath.resolve("libs");
        if (!Files.isDirectory(libsDir)) {
            System.err.println("Error: libs directory not found at " + libsDir);
            System.err.println(
                "The server must be started at least once to download " + "dependencies before the CLI can be used."
            );
            System.exit(1);
        }

        loadCachedDependencies(loader, libsDir);

        try {
            Class<?> clazz = loader.loadClass(SCHEMA_UPDATE_CLASS);
            SchemaUpdateCli runner = (SchemaUpdateCli) clazz.getDeclaredConstructor().newInstance();

            runner.run(configService, loggingService, resolvedPath);
        } catch (ReflectiveOperationException e) {
            System.err.println("Failed to initialize schema updater: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Schema update failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                loader.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Check that the Minecraft server is not currently running by attempting
     * to acquire the lock file. If the lock is held, the server is running.
     *
     * @param configPath The config directory path
     */
    private static void checkServerNotRunning(Path configPath) {
        Path lockFile = configPath.resolve(PrismLock.LOCK_FILE_NAME);
        if (!Files.exists(lockFile)) {
            return;
        }

        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.WRITE)) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                System.err.println("Error: The Minecraft server appears to be running.");
                System.err.println("Please stop the server before running schema updates.");
                System.exit(1);
            }
            lock.release();
        } catch (IOException e) {
            // Lock file exists but can't be opened — likely held by server
            System.err.println("Error: The Minecraft server appears to be running.");
            System.err.println("Please stop the server before running schema updates.");
            System.exit(1);
        }

        // Clean up stale lock file left over from a previous server run
        try {
            Files.deleteIfExists(lockFile);
        } catch (IOException e) {
            // Non-fatal — the file will be recreated by the server on next start
        }
    }

    /**
     * Load all cached dependency JARs from the libs directory into the classloader.
     *
     * @param loader The classloader to add JARs to
     * @param libsDir The libs directory containing cached dependency JARs
     */
    private static void loadCachedDependencies(JarInJarClassLoader loader, Path libsDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(libsDir, "*-remapped.jar")) {
            boolean found = false;
            for (Path jar : stream) {
                loader.addJarToClasspath(jar.toUri().toURL());
                found = true;
            }
            if (!found) {
                System.err.println("Error: No cached dependencies found in " + libsDir);
                System.err.println(
                    "The server must be started at least once to download " + "dependencies before the CLI can be used."
                );
                System.exit(1);
            }
        } catch (MalformedURLException e) {
            System.err.println("Error loading dependency: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading libs directory: " + e.getMessage());
            System.exit(1);
        }
    }
}
