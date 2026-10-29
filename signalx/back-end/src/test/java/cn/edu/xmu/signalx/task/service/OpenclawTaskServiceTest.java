package cn.edu.xmu.signalx.task.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.task.domain.dto.OpenclawTaskDTO;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository;
import cn.edu.xmu.signalx.task.domain.vo.OpenclawTaskVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OpenclawTaskServiceTest {

    @Autowired
    private OpenclawTaskService openclawTaskService;

    @Autowired
    private OpenclawTaskRepository openclawTaskRepository;

    private Long testUserId = 999L;

    @BeforeEach
    public void setUp() {
        // @Transactional ensures cleanup after each test
    }

    @Test
    public void testCreateAndGetOpenclawTask() {
        OpenclawTaskDTO.Create createCmd = new OpenclawTaskDTO.Create();
        createCmd.setName("Service Test Task");
        createCmd.setUrl("https://test.com/news");
        createCmd.setCrawlLogic("Test Logic");
        createCmd.setInterval(60);

        Long taskId = openclawTaskService.createOpenclawTask(testUserId, createCmd);
        assertNotNull(taskId);

        PageResult<OpenclawTaskVO> tasks = openclawTaskService.getOpenclawTasks(testUserId, 1, 10);
        assertEquals(1, tasks.getTotal());
        OpenclawTaskVO vo = tasks.getList().get(0);
        assertEquals("Service Test Task", vo.getName());
        assertEquals("https://test.com/news", vo.getUrl());
    }

    @Test
    public void testUpdateOpenclawTask() {
        OpenclawTaskDTO.Create createCmd = new OpenclawTaskDTO.Create();
        createCmd.setName("Original Name");
        createCmd.setUrl("https://test.com");
        createCmd.setCrawlLogic("Logic");
        createCmd.setInterval(60);
        Long taskId = openclawTaskService.createOpenclawTask(testUserId, createCmd);

        OpenclawTaskDTO.Update updateCmd = new OpenclawTaskDTO.Update();
        updateCmd.setName("Updated Name");
        updateCmd.setUrl("https://updated.com");
        updateCmd.setCrawlLogic("New Logic");
        updateCmd.setInterval(30);

        openclawTaskService.updateOpenclawTask(testUserId, taskId, updateCmd);

        var task = openclawTaskRepository.findById(taskId);
        assertEquals("Updated Name", task.getName());
        assertEquals("https://updated.com", task.getUrl());
        assertEquals(30, task.getConfig().getInterval());
    }

    @Test
    public void testSwitchStatus() {
        OpenclawTaskDTO.Create createCmd = new OpenclawTaskDTO.Create();
        createCmd.setName("Status Task");
        createCmd.setUrl("https://test.com");
        createCmd.setCrawlLogic("Status logic");
        createCmd.setInterval(60);
        Long taskId = openclawTaskService.createOpenclawTask(testUserId, createCmd);

        // Switch to Inactive
        openclawTaskService.switchOpenclawTask(testUserId, taskId, false);
        assertEquals(TaskStatus.INACTIVE, openclawTaskRepository.findById(taskId).getStatus());

        // Switch back to Active
        openclawTaskService.switchOpenclawTask(testUserId, taskId, true);
        assertEquals(TaskStatus.ACTIVE, openclawTaskRepository.findById(taskId).getStatus());
    }

    @Test
    public void testDeleteOpenclawTask() {
        OpenclawTaskDTO.Create createCmd = new OpenclawTaskDTO.Create();
        createCmd.setName("Delete Me");
        createCmd.setUrl("https://test.com");
        createCmd.setCrawlLogic("Delete logic");
        createCmd.setInterval(60);
        Long taskId = openclawTaskService.createOpenclawTask(testUserId, createCmd);

        openclawTaskService.deleteOpenclawTask(testUserId, taskId);

        assertNull(openclawTaskRepository.findById(taskId));
    }

    @Test
    public void testOwnershipValidation() {
        OpenclawTaskDTO.Create createCmd = new OpenclawTaskDTO.Create();
        createCmd.setName("Private Task");
        createCmd.setUrl("https://test.com");
        createCmd.setCrawlLogic("Private logic");
        createCmd.setInterval(60);
        Long taskId = openclawTaskService.createOpenclawTask(testUserId, createCmd);

        // Someone else tries to delete
        Long otherUserId = 888L;
        assertThrows(DomainException.class, () -> {
            openclawTaskService.deleteOpenclawTask(otherUserId, taskId);
        });
    }
}
