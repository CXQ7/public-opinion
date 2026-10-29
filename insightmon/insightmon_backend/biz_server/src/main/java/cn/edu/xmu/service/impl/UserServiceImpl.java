package cn.edu.xmu.service.impl;

import cn.edu.xmu.config.JwtProperties;
import cn.edu.xmu.domain.dto.LoginFormDTO;
import cn.edu.xmu.domain.po.User;
import cn.edu.xmu.domain.vo.UserLoginVO;
import cn.edu.xmu.enums.UserStatus;
import cn.edu.xmu.exception.BadRequestException;
import cn.edu.xmu.exception.ForbiddenException;
import cn.edu.xmu.mapper.UserMapper;
import cn.edu.xmu.service.UserService;
import cn.edu.xmu.utils.HashUtil;
import cn.edu.xmu.utils.JwtTool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;

    @Override
    public UserLoginVO login(LoginFormDTO loginDTO) {
        // 1.数据校验
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        // 2.根据用户名或手机号查询
        User user = lambdaQuery().eq(User::getUsername, username).one();
        Assert.notNull(user, "用户名错误");
        // 3.校验是否禁用
        if (user.getStatus() == UserStatus.FROZEN) {
            throw new ForbiddenException("用户被冻结");
        }
        // 4.校验密码
        String hashedInputPassword = HashUtil.hashPassword(password);
        if (!hashedInputPassword.equals(user.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        // 5.生成TOKEN
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        // 6.封装VO返回
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setToken(token);
        return vo;
    }

//    public void encodePassword(String rawPassword) {
//        String encodedPassword = passwordEncoder.encode(rawPassword);
//        log.info("原始密码: {}, 加密后密码: {}", rawPassword, encodedPassword);
//    }

}
