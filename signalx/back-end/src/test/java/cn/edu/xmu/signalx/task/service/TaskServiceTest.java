package cn.edu.xmu.signalx.task.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.task.domain.dto.TaskDTO;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import cn.edu.xmu.signalx.task.domain.vo.TaskVO;
import cn.edu.xmu.signalx.user.domain.dto.UserDTO;
import cn.edu.xmu.signalx.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private TaskRepository taskRepository;

    private Long testUserId;

    @BeforeEach
    public void setup() {
        // Prepare a user for task testing
        UserDTO.Add userAdd = new UserDTO.Add();
        userAdd.setUsername("task_tester_" + System.currentTimeMillis());
        userAdd.setPassword("123456");
        userAdd.setEmail("task_tester@example.com");
        testUserId = userService.add(userAdd);
    }

    @Test
    public void testCreateAndGetTask() {
        TaskDTO.Create createCmd = new TaskDTO.Create();
        createCmd.setKeywords(Arrays.asList("Java", "Spring"));
        createCmd.setInterval(60);
        createCmd.setStartTime(LocalDateTime.now());
        createCmd.setEndTime(LocalDateTime.now().plusDays(1));

        Long taskId = taskService.createTask(testUserId, createCmd);
        assertNotNull(taskId);

        // Verify Repository
        Task task = taskRepository.findById(taskId);
        assertNotNull(task);
        assertEquals(testUserId, task.getUserId());
        assertEquals(60, task.getConfig().getInterval());

        // Verify Service List
        PageResult<TaskVO> result = taskService.getTasks(testUserId, 1, 10);
        assertTrue(result.getTotal() > 0);
        TaskVO vo = result.getList().stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
        assertNotNull(vo);
        assertEquals(taskId, vo.getId());
    }

    @Test
    public void testUpdateTask() {
        // Create
        TaskDTO.Create createCmd = new TaskDTO.Create();
        createCmd.setKeywords(Arrays.asList("Init"));
        createCmd.setInterval(60);
        Long taskId = taskService.createTask(testUserId, createCmd);

        // Update
        TaskDTO.Update updateCmd = new TaskDTO.Update();
        updateCmd.setKeywords(Arrays.asList("Updated"));
        updateCmd.setInterval(120);

        taskService.updateTask(testUserId, taskId, updateCmd);

        // Verify
        Task task = taskRepository.findById(taskId);
        assertEquals("Updated", task.getKeywords().get(0));
        assertEquals(120, task.getConfig().getInterval());
    }

    @Test
    public void testSwitchStatus() {
        TaskDTO.Create createCmd = new TaskDTO.Create();
        createCmd.setKeywords(Arrays.asList("StatusTest"));
        createCmd.setInterval(60);
        Long taskId = taskService.createTask(testUserId, createCmd);

        // Default Active
        assertEquals(TaskStatus.ACTIVE, taskRepository.findById(taskId).getStatus());

        // Switch Off
        taskService.switchTask(testUserId, taskId, false);
        assertEquals(TaskStatus.INACTIVE, taskRepository.findById(taskId).getStatus());

        // Switch On
        taskService.switchTask(testUserId, taskId, true);
        assertEquals(TaskStatus.ACTIVE, taskRepository.findById(taskId).getStatus());
    }

    @Test
    public void testPermissionDenied() {
        // Task owned by testUserId
        TaskDTO.Create createCmd = new TaskDTO.Create();
        createCmd.setKeywords(Arrays.asList("Secret"));
        createCmd.setInterval(60);
        Long taskId = taskService.createTask(testUserId, createCmd);

        // Another user tries to update
        Long otherUserId = testUserId + 999;

        TaskDTO.Update updateCmd = new TaskDTO.Update();
        updateCmd.setKeywords(Arrays.asList("Hacked"));

        assertThrows(DomainException.class, () -> {
            taskService.updateTask(otherUserId, taskId, updateCmd);
        });
    }

    @Test
    public void testDeleteTask() {
        TaskDTO.Create createCmd = new TaskDTO.Create();
        createCmd.setKeywords(Arrays.asList("DeleteMe"));
        createCmd.setInterval(60);
        Long taskId = taskService.createTask(testUserId, createCmd);

        taskService.deleteTask(testUserId, taskId);

        // Depending on logic, findById might return null or return logically deleted entity
        // Since we didn't mock query/mapper heavily, assuming default MP behavior with logic delete
        assertNull(taskRepository.findById(taskId), "Task should not be found after delete");
    }
}
