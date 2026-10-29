package cn.edu.xmu.controller;

import cn.edu.xmu.domain.po.Config;
import cn.edu.xmu.service.ConfigService;
import cn.edu.xmu.utils.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.edu.xmu.result.Result;
import cn.edu.xmu.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "配置相关接口")
@RestController
@RequestMapping("/user/config")
@RequiredArgsConstructor
@CrossOrigin
public class ConfigController {

    private final ConfigService configService;

    @GetMapping("/{id}")
    public Result<Config> getConfigById(@PathVariable Long id) {
        Config config = configService.getById(id);
        if (config != null) {
            return Result.success(config);
        } else {
            return Result.error("配置未找到");
        }
    }

    @GetMapping("/list")
    public Result<PageResult> getConfigsList(
            @RequestParam(defaultValue = "1") int page,      // 页码，从1开始
            @RequestParam(defaultValue = "10") int pageSize, // 每页条数
            @RequestParam(required = false) String searchTerm // 搜索关键词
    ) {
        // 创建分页请求对象
        Page<Config> pageRequest = new Page<>(page, pageSize);

        // 使用 LambdaQueryWrapper 添加条件
        LambdaQueryWrapper<Config> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Config::getUserId, UserContext.getUser());
        if (searchTerm != null && !searchTerm.isEmpty()) {
            queryWrapper.like(Config::getName, searchTerm); // 模糊查询 name 字段
        }

        // 执行分页查询
        Page<Config> resultPage = configService.page(pageRequest, queryWrapper);

        // 封装为自定义 PageResult
        PageResult pageResult = new PageResult(resultPage.getTotal(), resultPage.getRecords());
        return Result.success(pageResult);
    }

    @PostMapping
    public Result<Boolean> createConfig(@RequestBody Config config) {
        config.setId(null);
        config.setUserId(UserContext.getUser());
        boolean success = configService.save(config);
        if (success) {
            return Result.success();
        } else {
            return Result.error("创建配置失败");
        }
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateConfig(@PathVariable Long id, @RequestBody Config config) {
        config.setId(id);
        boolean success = configService.updateById(config);
        if (success) {
            return Result.success();
        } else {
            return Result.error("更新配置失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteConfig(@PathVariable Long id) {
        boolean success = configService.removeById(id);
        if (success) {
            return Result.success();
        } else {
            return Result.error("删除配置失败");
        }
    }
}
