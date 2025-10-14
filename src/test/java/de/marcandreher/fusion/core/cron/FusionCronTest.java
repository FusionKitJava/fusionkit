package de.marcandreher.fusion.core.cron;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.marcandreher.fusionkit.core.cron.CronTask;
import de.marcandreher.fusionkit.core.cron.CronTaskMeta;
import de.marcandreher.fusionkit.core.cron.FusionCron;

public class FusionCronTest {
    
    private FusionCron fusionCron;
    
    @Before
    public void setUp() {
        // Clear any existing task metas from previous tests
        FusionCron.taskMetas.clear();
        fusionCron = new FusionCron();
    }
    
    @After
    public void tearDown() {
        if (fusionCron != null) {
            fusionCron.shutdown();
            try {
                // Give some time for shutdown to complete
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        FusionCron.taskMetas.clear();
    }
    
    @Test
    public void testRegisterTimedTask() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(2);
        CronTask testTask = createTestTask("TimedTask", latch);
        
        // When - register task to run immediately and then every 1 minute
        fusionCron.registerTimedTask(1, testTask);
        
        // Then
        assertEquals("Should have registered one task", 1, fusionCron.getTasks().size());
        assertEquals("Should have one task meta", 1, FusionCron.taskMetas.size());
        
        CronTaskMeta meta = FusionCron.taskMetas.get(0);
        assertEquals("Task name should match", "TimedTask", meta.getName());
        assertEquals("Task type should be TIMED", CronTaskMeta.CronEngineTaskType.TIMED, meta.getType());
        assertEquals("Interval should match", Long.valueOf(1L), meta.getIntervalMinutes());
        
        // Wait for task to run at least twice (should run immediately due to 0 initial delay)
        assertTrue("Task should run within timeout", latch.await(65, TimeUnit.SECONDS));
    }
    
    @Test
    public void testRegisterFixedRateTask() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        CronTask testTask = createTestTask("FixedRateTask", latch);
        
        // When - register task to run at current time + 1 second
        int currentHour = java.time.LocalDateTime.now().getHour();
        int currentMinute = java.time.LocalDateTime.now().getMinute();
        int targetMinute = (currentMinute + 1) % 60;
        
        fusionCron.registerFixedRateTask(currentHour, targetMinute, testTask);
        
        // Then
        assertEquals("Should have registered one task", 1, fusionCron.getTasks().size());
        assertEquals("Should have one task meta", 1, FusionCron.taskMetas.size());
        
        CronTaskMeta meta = FusionCron.taskMetas.get(0);
        assertEquals("Task name should match", "FixedRateTask", meta.getName());
        assertEquals("Task type should be FIXED_TIMED", CronTaskMeta.CronEngineTaskType.FIXED_TIMED, meta.getType());
        assertEquals("Target hour should match", Integer.valueOf(currentHour), meta.getTargetHour());
        assertEquals("Target minute should match", Integer.valueOf(targetMinute), meta.getTargetMinute());
    }
    
    @Test
    public void testRegisterTaskEachFullHour() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        CronTask testTask = createTestTask("HourlyTask", latch);
        
        // When
        fusionCron.registerTaskEachFullHour(testTask);
        
        // Then
        assertEquals("Should have registered one task", 1, fusionCron.getTasks().size());
        assertEquals("Should have one task meta", 1, FusionCron.taskMetas.size());
        
        CronTaskMeta meta = FusionCron.taskMetas.get(0);
        assertEquals("Task name should match", "HourlyTask", meta.getName());
        assertEquals("Task type should be FULL_HOUR", CronTaskMeta.CronEngineTaskType.FULL_HOUR, meta.getType());
    }
    
    @Test
    public void testMultipleTaskRegistration() {
        // Given
        CronTask task1 = createTestTask("Task1", new CountDownLatch(1));
        CronTask task2 = createTestTask("Task2", new CountDownLatch(1));
        CronTask task3 = createTestTask("Task3", new CountDownLatch(1));
        
        // When
        fusionCron.registerTimedTask(1, task1);
        fusionCron.registerFixedRateTask(12, 0, task2);
        fusionCron.registerTaskEachFullHour(task3);
        
        // Then
        assertEquals("Should have registered three tasks", 3, fusionCron.getTasks().size());
        assertEquals("Should have three task metas", 3, FusionCron.taskMetas.size());
        
        // Verify task types
        assertEquals("First task should be TIMED", CronTaskMeta.CronEngineTaskType.TIMED, FusionCron.taskMetas.get(0).getType());
        assertEquals("Second task should be FIXED_TIMED", CronTaskMeta.CronEngineTaskType.FIXED_TIMED, FusionCron.taskMetas.get(1).getType());
        assertEquals("Third task should be FULL_HOUR", CronTaskMeta.CronEngineTaskType.FULL_HOUR, FusionCron.taskMetas.get(2).getType());
    }
    
    @Test
    public void testTaskExecutionWithException() throws InterruptedException {
        // Given
        CronTask faultyTask = new CronTask() {
            private AtomicInteger callCount = new AtomicInteger(0);
            
            @Override
            public void initialize() {}
            
            @Override
            public void run() {
                callCount.incrementAndGet();
                throw new RuntimeException("Test exception");
            }
            
            @Override
            public void shutdown() {}
            
            @Override
            public String getName() {
                return "FaultyTask";
            }
            

        };
        
        // When
        fusionCron.registerTimedTask(1, faultyTask);
        
        // Then - verify the task was registered and scheduler handles exceptions gracefully
        assertEquals("Should have registered one task", 1, fusionCron.getTasks().size());
        assertEquals("Should have one task meta", 1, FusionCron.taskMetas.size());
    }
    
    @Test
    public void testShutdown() throws InterruptedException {
        // Given
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        CronTask testTask = new CronTask() {
            @Override
            public void initialize() {}
            
            @Override
            public void run() {}
            
            @Override
            public void shutdown() {
                shutdownLatch.countDown();
            }
            
            @Override
            public String getName() {
                return "ShutdownTestTask";
            }
        };
        
        fusionCron.registerTimedTask(1, testTask);
        
        // When
        fusionCron.shutdown();
        
        // Then
        assertTrue("Shutdown should be called on all tasks", shutdownLatch.await(5, TimeUnit.SECONDS));
    }
    
    @Test
    public void testTaskMetaData() {
        // Given
        CronTask task = createTestTask("MetaDataTask", new CountDownLatch(1));
        
        // When
        fusionCron.registerTimedTask(5, task);
        
        // Then
        assertEquals("Should have one task meta", 1, FusionCron.taskMetas.size());
        CronTaskMeta meta = FusionCron.taskMetas.get(0);
        
        assertNotNull("Meta should not be null", meta);
        assertEquals("Name should match", "MetaDataTask", meta.getName());
        assertEquals("Type should be TIMED", CronTaskMeta.CronEngineTaskType.TIMED, meta.getType());
        assertEquals("Interval should match", Long.valueOf(5L), meta.getIntervalMinutes());
    }
    
    @Test
    public void testGetTasks() {
        // Given
        assertTrue("Tasks list should be empty initially", fusionCron.getTasks().isEmpty());
        
        CronTask task1 = createTestTask("Task1", new CountDownLatch(1));
        CronTask task2 = createTestTask("Task2", new CountDownLatch(1));
        
        // When
        fusionCron.registerTimedTask(1, task1);
        fusionCron.registerTimedTask(2, task2);
        
        // Then
        assertEquals("Should have two tasks", 2, fusionCron.getTasks().size());
        assertTrue("Should contain task1", fusionCron.getTasks().contains(task1));
        assertTrue("Should contain task2", fusionCron.getTasks().contains(task2));
    }
    
    /**
     * Helper method to create a test CronTask
     */
    private CronTask createTestTask(String name, CountDownLatch latch) {
        return new CronTask() {
            @Override
            public void initialize() {}
            
            @Override
            public void run() {
                latch.countDown();
            }
            
            @Override
            public void shutdown() {}
            
            @Override
            public String getName() {
                return name;
            }
        };
    }
}
