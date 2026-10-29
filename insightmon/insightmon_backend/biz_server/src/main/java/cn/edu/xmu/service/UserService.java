package cn.edu.xmu.service;

import cn.edu.xmu.domain.dto.LoginFormDTO;
import cn.edu.xmu.domain.po.User;
import cn.edu.xmu.domain.vo.UserLoginVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {

    UserLoginVO login(LoginFormDTO loginFormDTO);
}
