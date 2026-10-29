package cn.edu.xmu.signalx.user.controller;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.user.domain.dto.UserDTO;
import cn.edu.xmu.signalx.user.domain.vo.UserLoginVO;
import cn.edu.xmu.signalx.user.domain.vo.UserVO;
import cn.edu.xmu.signalx.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Valid UserDTO.Login dto) {
        return Result.success(userService.login(dto));
    }

    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(@Valid UserDTO.Query dto) {
        return Result.success(userService.getList(dto));
    }

    @PostMapping("/add")
    public Result<Map<String, Long>> add(@RequestBody @Valid UserDTO.Add dto) {
        Long id = userService.add(dto);
        return Result.success(Map.of("id", id));
    }

    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody @Valid UserDTO.Update dto) {
        return Result.success(userService.update(dto));
    }

    @PostMapping("/delete")
    public Result<Boolean> delete(@RequestBody Map<String, Long> params) {
        Long id = params.get("id");
        if (id == null) {
            return Result.fail(400, "id is required");
        }
        UserDTO.DeleteDTO dto = new UserDTO.DeleteDTO();
        dto.setId(id);
        return Result.success(userService.delete(dto));
    }
}
