package com.solidoa.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.system.entity.User;
import com.solidoa.system.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User selectByUsername(@Param("username") String username);

    List<UserVO> selectPageList(@Param("offset") int offset,
                               @Param("limit") int limit,
                               @Param("username") String username,
                               @Param("realName") String realName);

    long selectCount(@Param("username") String username,
                     @Param("realName") String realName);

    List<Long> selectUserIdsByDeptIds(@Param("deptIds") List<Long> deptIds);
}