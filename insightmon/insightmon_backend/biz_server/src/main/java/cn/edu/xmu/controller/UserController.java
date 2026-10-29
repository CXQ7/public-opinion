package cn.edu.xmu.controller;

import cn.edu.xmu.domain.dto.LoginFormDTO;
import cn.edu.xmu.domain.vo.UserLoginVO;
import cn.edu.xmu.result.Result;
import cn.edu.xmu.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户相关接口")
@RestController
@RequestMapping("/user/user")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户登录接口")
    @PostMapping("login")
    public Result<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return Result.success(userService.login(loginFormDTO));
    }

}
