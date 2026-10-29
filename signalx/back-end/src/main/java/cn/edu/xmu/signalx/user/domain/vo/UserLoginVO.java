package cn.edu.xmu.signalx.user.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginVO {
    private String token;
    private Long id;
    private String username;
    private String email;
    private String role;
}
