package cn.edu.xmu.signalx.user.infrastructure.converter;

import cn.edu.xmu.signalx.user.domain.model.User;
import cn.edu.xmu.signalx.user.domain.model.UserRole;
import cn.edu.xmu.signalx.user.domain.model.UserStatus;
import cn.edu.xmu.signalx.user.domain.vo.UserVO;
import cn.edu.xmu.signalx.user.infrastructure.po.UserPO;
import org.springframework.stereotype.Component;

@Component
public class UserConverter {

    public static User toDomain(UserPO po) {
        if (po == null) {
            return null;
        }
        return User.reconstruct(
                po.getId(),
                po.getUsername(),
                po.getPassword(),
                po.getEmail(),
                UserRole.parse(po.getRole()),
                UserStatus.fromCode(po.getStatus())
        );
    }

    public static UserPO toPO(User user) {
        if (user == null) {
            return null;
        }
        UserPO po = new UserPO();
        po.setId(user.getId());
        po.setUsername(user.getUsername());
        po.setPassword(user.getPasswordHash());
        po.setEmail(user.getEmail());
        if (user.getRole() != null) {
            po.setRole(user.getRole().name());
        }
        if (user.getStatus() != null) {
            po.setStatus(user.getStatus().getCode());
        }
        return po;
    }

    public static UserVO toVO(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        if (user.getRole() != null) vo.setRole(user.getRole().name());
        if (user.getStatus() != null) vo.setStatus(user.getStatus().getCode());
        return vo;
    }
}
