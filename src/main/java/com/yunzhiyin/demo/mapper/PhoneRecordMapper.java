package com.yunzhiyin.demo.mapper;

import com.yunzhiyin.demo.pojo.PhoneRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PhoneRecordMapper {
    void insert(@Param("phoneRecords") List<PhoneRecord> phoneRecords);

    List<PhoneRecord> getHistoryByUserName(String userName);

    /**
     * 查询还没有通话内容的记录
     * @return
     */
    List<PhoneRecord> getNoContent();

    void updateHaveResult(@Param("phoneRecords") List<PhoneRecord> phoneRecords);

    void updateHaveResultOne(@Param("phoneRecord") PhoneRecord phoneRecord);

    void updateHaveRadio(@Param("phoneRecords") List<PhoneRecord> phoneRecords);

    List<PhoneRecord> getNoRadio();

    List<PhoneRecord> getUpdataState();

    void updateState(@Param("phoneRecords") List<PhoneRecord> phoneRecords);


}
