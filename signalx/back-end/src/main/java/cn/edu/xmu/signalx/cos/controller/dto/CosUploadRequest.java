// src\main\java\cn\edu\xmu\signalx\cos\controller\dto\CosUploadRequest.java
package cn.edu.xmu.signalx.cos.controller.dto;

import lombok.Data;

@Data
public class CosUploadRequest {
    private String title;
    private String content;
    private String userId;
}
