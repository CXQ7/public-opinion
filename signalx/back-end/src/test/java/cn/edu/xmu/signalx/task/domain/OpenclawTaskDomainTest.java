package cn.edu.xmu.signalx.task.domain;

import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class OpenclawTaskDomainTest {

    @Test
    public void testCreateOpenclawTaskSuccess() {
        Long userId = 100L;
        String name = "Test Task";
        String url = "https://example.com/news";
        String logic = "Extract headlines from the main page.";
        Integer interval = 30;

        OpenclawTask task = OpenclawTask.create(userId, name, url, logic, null, null, interval);

        assertNotNull(task);
        assertEquals(userId, task.getUserId());
        assertEquals(name, task.getName());
        assertEquals(url, task.getUrl());
        assertEquals(logic, task.getCrawlLogic());
        assertEquals(interval, task.getConfig().getInterval());
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
    }

    @Test
    public void testCreateOpenclawTaskInvalidUrl() {
        // Missing protocol
        assertThrows(DomainException.class, () -> {
            OpenclawTask.create(1L, "Test", "example.com", "logic", null, null, 60);
        });
    }

    @Test
    public void testUpdateOpenclawTask() {
        OpenclawTask task = OpenclawTask.create(1L, "Old Name", "https://old.com", "old logic", null, null, 60);

        task.update("New Name", "https://new.com", "new logic", 120);

        assertEquals("New Name", task.getName());
        assertEquals("https://new.com", task.getUrl());
        assertEquals("new logic", task.getCrawlLogic());
        assertEquals(120, task.getConfig().getInterval());
    }

    @Test
    public void testSwitchStatus() {
        OpenclawTask task = OpenclawTask.create(1L, "Test", "https://test.com", "logic", null, null, 60);

        task.switchStatus(false);
        assertEquals(TaskStatus.INACTIVE, task.getStatus());
        assertNull(task.getNextRunTime());

        task.switchStatus(true);
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
        assertNotNull(task.getNextRunTime());
    }
}
