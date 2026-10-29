package cn.edu.xmu.signalx.user.service;

import cn.edu.xmu.signalx.core.common.ErrorCode;
import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.core.util.JwtUtils;
import cn.edu.xmu.signalx.user.domain.dto.UserDTO;
import cn.edu.xmu.signalx.user.domain.model.User;
import cn.edu.xmu.signalx.user.domain.repository.UserRepository;
import cn.edu.xmu.signalx.user.domain.vo.UserLoginVO;
import cn.edu.xmu.signalx.user.domain.vo.UserVO;
import cn.edu.xmu.signalx.user.infrastructure.converter.UserConverter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserLoginVO login(UserDTO.Login dto) {
        User user = userRepository.findByUsername(dto.getUsername());
        if (user == null || !user.verifyPassword(dto.getPassword())) {
            throw new DomainException(ErrorCode.USER_PASSWORD_ERROR, "Invalid username or password");
        }

        // Generate real JWT token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        return UserLoginVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .token(token)
                .build();
    }

    @Transactional
    public Long add(UserDTO.Add dto) {
        User existing = userRepository.findByUsername(dto.getUsername());
        if (existing != null) {
            throw new DomainException(ErrorCode.USER_ALREADY_EXISTS, "Username already exists");
        }
        // Force creation of NORMAL user, ignoring dto.getRole()
        User user = User.createNormal(dto.getUsername(), dto.getPassword(), dto.getEmail());
        return userRepository.save(user);
    }

    @Transactional
    public Boolean update(UserDTO.Update dto) {
        User user = userRepository.findById(dto.getId());
        if (user == null) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
        // Update basic info only, ignoring dto.getRole()
        user.updateBasicInfo(dto.getUsername(), dto.getEmail());
        userRepository.save(user);
        return true;
    }

    @Transactional
    public Boolean delete(UserDTO.DeleteDTO dto) {
        if (userRepository.findById(dto.getId()) == null) {
            throw new DomainException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
        return userRepository.remove(dto.getId());
    }

    public PageResult<UserVO> getList(UserDTO.Query dto) {
        Page<User> page = userRepository.queryList(dto.getPage(), dto.getSize(), dto.getUserId());

        List<UserVO> voList = page.getRecords().stream()
                .map(UserConverter::toVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, page.getTotal());
    }
}
