package cn.edu.xmu.signalx.user.domain.dto;

import cn.edu.xmu.signalx.core.dto.BasePageDTO;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class UserDTO {

    @Data
    public static class Login {
        @NotBlank(message = "Username required")
        private String username;
        @NotBlank(message = "Password required")
        private String password;
    }

    @Data
    public static class Add {
        @NotBlank(message = "Username required")
        private String username;
        @NotBlank(message = "Password required")
        private String password;
        @Email(message = "Invalid email format")
        private String email;
        private String role; // Will be ignored by domain logic
    }

    @Data
    public static class Update {
        @NotNull(message = "ID required")
        private Long id;
        private String username;
        private String email;
        private String role; // Will be ignored by domain logic
    }

    @Data
    public static class DeleteDTO {
        @NotNull(message = "ID required")
        private Long id;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Query extends BasePageDTO {
        private Long userId;
    }
}
