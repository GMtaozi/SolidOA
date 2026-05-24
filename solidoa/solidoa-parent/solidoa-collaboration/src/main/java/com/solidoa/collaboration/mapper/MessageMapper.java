package com.solidoa.collaboration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.solidoa.collaboration.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    List<Message> selectPageList(@Param("offset") int offset,
                                 @Param("limit") int limit,
                                 @Param("receiverId") Long receiverId);

    long selectCount(@Param("receiverId") Long receiverId);

    long selectUnreadCount(@Param("receiverId") Long receiverId);
}