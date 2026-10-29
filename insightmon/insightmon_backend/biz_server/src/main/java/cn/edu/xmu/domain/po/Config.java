package cn.edu.xmu.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("config")
@Schema(description = "配置对象")
public class Config implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "事件唯一ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "检索关键字")
    private String searchKey;

    @Schema(description = "到期日")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireDate;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
