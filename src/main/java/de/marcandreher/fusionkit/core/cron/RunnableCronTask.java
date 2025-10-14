package de.marcandreher.fusionkit.core.cron;

import org.slf4j.Logger;

import de.marcandreher.fusionkit.core.FusionKit;

public class RunnableCronTask implements CronTask {

    protected Logger logger = FusionKit.getLogger(RunnableCronTask.class, getName());

    @Override
    public void initialize() { }

    @Override
    public void run() { }

    @Override
    public void shutdown() { }

    @Override
    public String getName() {
        return "RunnableCronTask";
    }

}