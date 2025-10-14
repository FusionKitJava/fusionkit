package de.marcandreher.fusionkit.core.cron;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;

public class FusionCron {
    private final static int DEFAULT_THREAD_POOL_SIZE = 5;
    private final Logger logger = FusionKit.getLogger(FusionCron.class);
    private ScheduledExecutorService scheduler;
    private final List<CronTask> tasks = new ArrayList<>();

    public static final List<CronTaskMeta> taskMetas = new ArrayList<>();
    
    private static class FusionKitThreadFactory implements ThreadFactory {
        private final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        FusionKitThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = "FK-Cron-" + poolNumber.getAndIncrement() + "-Thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    public FusionCron() {
        this.scheduler = Executors.newScheduledThreadPool(DEFAULT_THREAD_POOL_SIZE, new FusionKitThreadFactory());
    }

    public FusionCron(int threadPoolSize) {
        this.scheduler = Executors.newScheduledThreadPool(threadPoolSize, new FusionKitThreadFactory());
    }
    
    /**
     * Runs task every X minutes.
     */
    public void registerTimedTask(long intervalMinutes, CronTask task) {
        ensureSchedulerAvailable();
        tasks.add(task);
        logger.debug("Registering timed task: " + task.getName() + " to run every " + intervalMinutes + " minutes");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.debug("Running task: " + task.getName());
                task.run();
            } catch (Exception e) {
                logger.error("Error running task: " + task.getName(), e);
            }
        }, 0, intervalMinutes, TimeUnit.MINUTES);

        CronTaskMeta meta = new CronTaskMeta();
        meta.setName(task.getName());
        meta.setType(CronTaskMeta.CronEngineTaskType.TIMED);
        meta.setIntervalMinutes(intervalMinutes);
        taskMetas.add(meta);
    }

    /**
     * Runs task every day at specified hour:minute.
     */
    public void registerFixedRateTask(int targetHour, int targetMinute, CronTask task) {
        ensureSchedulerAvailable();
        tasks.add(task);
        logger.debug("Registering fixed rate task: " + task.getName() + " to run every day at " + targetHour + ":"
                + targetMinute);

        long initialDelay = computeInitialDelay(targetHour, targetMinute);
        long oneDay = TimeUnit.DAYS.toSeconds(1);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.debug("Running task: " + task.getName());
                task.run();
            } catch (Exception e) {
                logger.error("Error running task: " + task.getName(), e);
            }
        }, initialDelay, oneDay, TimeUnit.SECONDS);

        CronTaskMeta meta = new CronTaskMeta();
        meta.setName(task.getName());
        meta.setType(CronTaskMeta.CronEngineTaskType.FIXED_TIMED);
        meta.setTargetHour(targetHour);
        meta.setTargetMinute(targetMinute);
        taskMetas.add(meta);
    }

    public void registerTaskEachFullHour(CronTask task) {
        ensureSchedulerAvailable();
        logger.debug("Registering task: " + task.getName() + " to run every full hour");

        long initialDelay = fullHourDelay();
        long oneHour = TimeUnit.HOURS.toSeconds(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                logger.debug("Running task: " + task.getName());
                task.run();
            }
            catch (Exception e) {
                logger.error("Error running task: " + task.getName(), e);
            }
        }, initialDelay, oneHour, TimeUnit.SECONDS);

        CronTaskMeta meta = new CronTaskMeta();
        meta.setName(task.getName());
        meta.setType(CronTaskMeta.CronEngineTaskType.FULL_HOUR);
        taskMetas.add(meta);
    }

    public List<CronTask> getTasks() {
        return tasks;
    }

    private long fullHourDelay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return Duration.between(now, nextRun).getSeconds();
    }

    private long computeInitialDelay(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour).withMinute(minute).withSecond(0);

        if (now.isAfter(nextRun)) {
            nextRun = nextRun.plusDays(1); // Schedule for tomorrow
        }

        return Duration.between(now, nextRun).getSeconds();
    }

    private void ensureSchedulerAvailable() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = java.util.concurrent.Executors.newScheduledThreadPool(DEFAULT_THREAD_POOL_SIZE, new FusionKitThreadFactory());
        }
    }

    public void shutdown() {
        for (CronTask task : tasks) {
            task.shutdown();
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}