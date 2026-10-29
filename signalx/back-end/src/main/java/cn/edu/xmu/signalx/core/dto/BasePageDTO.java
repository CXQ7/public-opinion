package cn.edu.xmu.signalx.core.dto;

import lombok.Data;

@Data
public class BasePageDTO {
    private Integer page = 1;
    private Integer size = 10;
}
