package cn.edu.xmu.signalx.task.domain;

import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskDomainTest {

    @Test
    public void testCreateTaskSuccess() {
        Long userId = 100L;
        List<String> keywords = Arrays.asList("Apple", "iPhone");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        Integer interval = 60;

        Task task = Task.create(userId, keywords, start, end, interval);

        assertNotNull(task);
        assertEquals(userId, task.getUserId());
        assertEquals(keywords, task.getKeywords());
        assertEquals(start, task.getConfig().getStartTime());
        assertEquals(end, task.getConfig().getEndTime());
        assertEquals(60, task.getConfig().getInterval());
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
    }

    @Test
    public void testCreateTaskInvalidKeywords() {
        // Empty keywords
        Assertions.assertThrows(DomainException.class, () -> {
            Task.create(1L, Collections.emptyList(), null, null, 60);
        });

        // Too many keywords
        List<String> manyKeywords = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            manyKeywords.add("k" + i);
        }
        Assertions.assertThrows(DomainException.class, () -> {
            Task.create(1L, manyKeywords, null, null, 60);
        });
    }

    @Test
    public void testCreateTaskInvalidTimeRange() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(1); // End before Start

        Assertions.assertThrows(DomainException.class, () -> {
            Task.create(1L, Arrays.asList("test"), start, end, 60);
        });
    }

    @Test
    public void testValidationOwnership() {
        Task task = Task.create(1L, Arrays.asList("test"), null, null, 60);

        assertDoesNotThrow(() -> task.validateOwnership(1L));

        assertThrows(DomainException.class, () -> task.validateOwnership(2L));
    }

    @Test
    public void testUpdateConfig() {
        Task task = Task.create(1L, Arrays.asList("test"), null, null, 60);

        List<String> newKeywords = Arrays.asList("updated");
        Integer newInterval = 30;

        task.updateConfig(newKeywords, null, null, newInterval);

        assertEquals(newKeywords, task.getKeywords());
        assertEquals(30, task.getConfig().getInterval());
    }
}
