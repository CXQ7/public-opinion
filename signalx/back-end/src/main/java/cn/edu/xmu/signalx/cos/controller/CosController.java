package cn.edu.xmu.signalx.cos.controller;

import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.cos.controller.dto.CosDeleteRequest;
import cn.edu.xmu.signalx.cos.controller.dto.CosUploadRequest;
import cn.edu.xmu.signalx.cos.service.CosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * COS存储管理控制器
 */
@Slf4j
@Tag(name = "cos-controller")
@RestController
@RequestMapping("/api/v1/cos")
@RequiredArgsConstructor
public class CosController {

    private final CosService cosService;

    @Operation(summary = "检查COS服务状态")
    @GetMapping("/status")
    public Result<Boolean> getStatus() {
        boolean available = cosService.isAvailable();
        return Result.success(available);
    }

    @Operation(summary = "存储舆情报告")
    @PostMapping("/store/report")
    public Result<String> storeReport(@RequestBody @Valid CosUploadRequest request) {
        String fileUrl = cosService.storeReport(
            request.getTitle(),
            request.getContent(),
            request.getUserId()
        );
        return Result.success(fileUrl);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<Boolean> deleteFile(@RequestBody @Valid CosDeleteRequest request) {
        boolean success = cosService.deleteFile(request.getFilePath());
        return success ? Result.success(true) : Result.fail(10001, "删除失败");
    }
}
