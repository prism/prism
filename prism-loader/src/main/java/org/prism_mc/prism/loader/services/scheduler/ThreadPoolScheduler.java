/*
 * This file is part of LuckPerms, licensed under the MIT License.
 * It has been modified for use in Prism.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package org.prism_mc.prism.loader.services.scheduler;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.prism_mc.prism.loader.services.dependencies.loader.PluginLoader;

/**
 * A little utility for thread pools, primarily used for library downloads.
 */
public class ThreadPoolScheduler {

    private static final int PARALLELISM = 16;

    /**
     * The plugin loader.
     */
    private final PluginLoader loader;

    /**
     * The scheduler.
     */
    private final ScheduledThreadPoolExecutor scheduler;

    /**
     * The worker.
     */
    private final ForkJoinPool worker;

    /**
     * Constructor.
     *
     * @param loader The plugin loader
     */
    public ThreadPoolScheduler(PluginLoader loader) {
        this.loader = loader;

        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("prism-scheduler");
            return thread;
        });
        this.scheduler.setRemoveOnCancelPolicy(true);
        this.scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.worker = new ForkJoinPool(PARALLELISM, new WorkerThreadFactory(), new ExceptionHandler(), false);
    }

    public Executor async() {
        return this.worker;
    }

    /**
     * Shutdown the scheduler.
     */
    public void shutdownScheduler() {
        this.scheduler.shutdown();
        try {
            if (!this.scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                this.loader.loggingService().warn("Timed out waiting for the Prism scheduler to terminate");
                reportRunningTasks(thread -> thread.getName().equals("prism-scheduler"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdown the executor.
     */
    public void shutdownExecutor() {
        this.worker.shutdown();
        try {
            if (!this.worker.awaitTermination(1, TimeUnit.MINUTES)) {
                this.loader.loggingService().warn("Timed out waiting for the Prism worker thread pool to terminate");
                reportRunningTasks(thread -> thread.getName().startsWith("prism-worker-"));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void reportRunningTasks(Predicate<Thread> predicate) {
        Thread.getAllStackTraces().forEach((thread, stack) -> {
            if (predicate.test(thread)) {
                this.loader.loggingService().warn(
                    "Thread " +
                        thread.getName() +
                        " is blocked, and may be the reason for the slow shutdown!\n" +
                        Arrays.stream(stack)
                            .map(el -> "  " + el)
                            .collect(Collectors.joining("\n"))
                );
            }
        });
    }

    private static final class WorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

        private static final AtomicInteger COUNT = new AtomicInteger(0);

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setDaemon(true);
            thread.setName("prism-worker-" + COUNT.getAndIncrement());
            return thread;
        }
    }

    private final class ExceptionHandler implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            ThreadPoolScheduler.this.loader.loggingService().error(
                "Thread {0} threw an uncaught exception",
                t.getName()
            );
            ThreadPoolScheduler.this.loader.loggingService().error(e.getMessage());
        }
    }
}
