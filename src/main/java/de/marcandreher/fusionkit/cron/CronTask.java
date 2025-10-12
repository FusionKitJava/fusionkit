package de.marcandreher.fusionkit.cron;

public interface CronTask {

    public void initialize();

    public void run();

    public void shutdown();

    public String getName();
}