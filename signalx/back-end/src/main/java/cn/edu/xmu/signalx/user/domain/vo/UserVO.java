package cn.edu.xmu.signalx.user.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Integer status;
}
