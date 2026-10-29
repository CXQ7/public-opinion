package cn.edu.xmu.signalx.user.domain.repository;

import cn.edu.xmu.signalx.user.domain.model.User;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserRepository {
    User findByUsername(String username);
    User findById(Long id);
    Long save(User user);
    boolean remove(Long id);
    Page<User> queryList(Integer page, Integer size, Long userId);
}
