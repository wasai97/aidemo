package com.yunzhiyin.demo.mapper;

import com.yunzhiyin.demo.pojo.Content;
import org.apache.ibatis.annotations.Param;

public interface ContentMapper {
    void insert(@Param("content") Content content);

    Content getDetailContent(@Param("dutyId") String dutyID, @Param("phoneNumber") String phoneNumber);
}
