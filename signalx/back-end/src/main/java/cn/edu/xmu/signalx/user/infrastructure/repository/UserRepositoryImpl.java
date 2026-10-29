package cn.edu.xmu.signalx.user.infrastructure.repository;

import cn.edu.xmu.signalx.user.domain.model.User;
import cn.edu.xmu.signalx.user.domain.repository.UserRepository;
import cn.edu.xmu.signalx.user.infrastructure.converter.UserConverter;
import cn.edu.xmu.signalx.user.infrastructure.mapper.UserMapper;
import cn.edu.xmu.signalx.user.infrastructure.po.UserPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        UserPO po = userMapper.selectOne(new LambdaQueryWrapper<UserPO>()
                .eq(UserPO::getUsername, username));
        return UserConverter.toDomain(po);
    }

    @Override
    public User findById(Long id) {
        UserPO po = userMapper.selectById(id);
        return UserConverter.toDomain(po);
    }

    @Override
    public Long save(User user) {
        UserPO po = UserConverter.toPO(user);
        if (po.getId() == null) {
            userMapper.insert(po);
            user.setId(po.getId()); // Update domain ID
        } else {
            userMapper.updateById(po);
        }
        return po.getId();
    }

    @Override
    public boolean remove(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public Page<User> queryList(Integer page, Integer size, Long userId) {
        Page<UserPO> poPage = new Page<>(page, size);
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserPO::getId, userId);
        }
        userMapper.selectPage(poPage, wrapper);

        Page<User> domainPage = new Page<>(poPage.getCurrent(), poPage.getSize(), poPage.getTotal());
        List<User> userList = poPage.getRecords().stream()
                .map(UserConverter::toDomain)
                .collect(Collectors.toList());
        domainPage.setRecords(userList);
        return domainPage;
    }
}
