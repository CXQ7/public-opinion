package cn.edu.xmu.signalx.user.service;

import cn.edu.xmu.signalx.core.common.ErrorCode;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.user.domain.dto.UserDTO;
import cn.edu.xmu.signalx.user.domain.model.User;
import cn.edu.xmu.signalx.user.domain.repository.UserRepository;
import cn.edu.xmu.signalx.user.domain.vo.UserLoginVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testAddUser() {
        UserDTO.Add addDTO = new UserDTO.Add();
        addDTO.setUsername("test_unit_01_" + UUID.randomUUID());
        addDTO.setPassword("pwd123");
        addDTO.setEmail("unit01_" + UUID.randomUUID() + "@test.com");
        addDTO.setRole("ADMIN");

        Long id = userService.add(addDTO);
        assertNotNull(id);

        User user = userRepository.findById(id);
        assertNotNull(user);
        assertEquals(addDTO.getUsername(), user.getUsername());
        assertEquals("USER", user.getRole().name(), "Role should fail to default USER on registration");
        assertTrue(user.verifyPassword("pwd123"), "Password verification should pass");
    }

    @Test
    public void testAddUserDuplicate() {
        UserDTO.Add addDTO = new UserDTO.Add();
        String dupName = "test_dup_" + UUID.randomUUID();
        addDTO.setUsername(dupName);
        addDTO.setPassword("123");
        userService.add(addDTO);

        UserDTO.Add dupDTO = new UserDTO.Add();
        dupDTO.setUsername(dupName);
        dupDTO.setPassword("456");

        DomainException exception = assertThrows(DomainException.class, () -> {
            userService.add(dupDTO);
        });
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getCode());
    }

    @Test
    public void testLogin() {
        UserDTO.Add addDTO = new UserDTO.Add();
        addDTO.setUsername("test_login_" + UUID.randomUUID());
        addDTO.setPassword("correct_pwd");
        userService.add(addDTO);

        UserDTO.Login wrongLogin = new UserDTO.Login();
        wrongLogin.setUsername(addDTO.getUsername());
        wrongLogin.setPassword("wrong_pwd");

        DomainException e = assertThrows(DomainException.class, () -> userService.login(wrongLogin));
        assertEquals(ErrorCode.USER_PASSWORD_ERROR, e.getCode());

        UserDTO.Login correctLogin = new UserDTO.Login();
        correctLogin.setUsername(addDTO.getUsername());
        correctLogin.setPassword("correct_pwd");

        UserLoginVO vo = userService.login(correctLogin);
        assertNotNull(vo.getToken(), "Token should be returned on successful login");
        assertEquals(addDTO.getUsername(), vo.getUsername());
    }

    @Test
    public void testUpdate() {
        UserDTO.Add addDTO = new UserDTO.Add();
        addDTO.setUsername("test_update_" + UUID.randomUUID());
        addDTO.setPassword("123");
        Long id = userService.add(addDTO);

        UserDTO.Update updateDTO = new UserDTO.Update();
        updateDTO.setId(id);
        updateDTO.setEmail("new@email.com");
        updateDTO.setRole("ADMIN");

        userService.update(updateDTO);

        User user = userRepository.findById(id);
        assertEquals("new@email.com", user.getEmail());
        assertEquals("USER", user.getRole().name(), "Role should not be updated via regular update interface");
    }

    @Test
    public void testDelete() {
        UserDTO.Add addDTO = new UserDTO.Add();
        addDTO.setUsername("test_delete_" + UUID.randomUUID());
        addDTO.setPassword("123");
        Long id = userService.add(addDTO);

        UserDTO.DeleteDTO deleteDTO = new UserDTO.DeleteDTO();
        deleteDTO.setId(id);
        userService.delete(deleteDTO);

        User user = userRepository.findById(id);
        assertNull(user, "User should be implicitly deleted and not retrievable via standard find");
    }

    @Test
    public void testAddUserInvalidInput() {
        UserDTO.Add addDTO = new UserDTO.Add();
        addDTO.setUsername(" ");
        addDTO.setPassword("123");
        assertThrows(DomainException.class, () -> userService.add(addDTO));

        UserDTO.Add addDTO2 = new UserDTO.Add();
        addDTO2.setUsername("user_blank_pwd_" + UUID.randomUUID());
        addDTO2.setPassword(" ");
        assertThrows(DomainException.class, () -> userService.add(addDTO2));
    }

    @Test
    public void testLoginUserNotFound() {
        UserDTO.Login login = new UserDTO.Login();
        login.setUsername("not_exist_" + UUID.randomUUID());
        login.setPassword("123");

        DomainException e = assertThrows(DomainException.class, () -> userService.login(login));
        assertEquals(ErrorCode.USER_PASSWORD_ERROR, e.getCode());
    }

    @Test
    public void testUpdateUserNotFound() {
        UserDTO.Update updateDTO = new UserDTO.Update();
        updateDTO.setId(-1L);
        updateDTO.setEmail("none@test.com");

        DomainException e = assertThrows(DomainException.class, () -> userService.update(updateDTO));
        assertEquals(ErrorCode.USER_NOT_FOUND, e.getCode());
    }

    @Test
    public void testDeleteUserNotFound() {
        UserDTO.DeleteDTO deleteDTO = new UserDTO.DeleteDTO();
        deleteDTO.setId(-1L);

        DomainException e = assertThrows(DomainException.class, () -> userService.delete(deleteDTO));
        assertEquals(ErrorCode.USER_NOT_FOUND, e.getCode());
    }
}
