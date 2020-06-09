package com.yunzhiyin.demo.mapper;

import com.yunzhiyin.demo.pojo.Radio;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RadioMapper {
    void insert(@Param("radios") List<Radio> radios);

    String getRadio(@Param("dutyId") String dutyId, @Param("phoneNumber") String phoneNumber, @Param("id") String id);
}
