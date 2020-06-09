package com.yunzhiyin.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhoneRecord {
    private String dutyId;
    private Integer id;
    private String phoneNumber;
    private String callDate;
    private String userName;
    //判断拨打电话是否成功：0：失败 1：成功
    private Integer isSuccess;
    //判断是否有通话内容 0:无通话内容  1：有通话内容
    private Integer haveResult;
    private Integer haveRadio;//判断是否有音频
    private Integer state;//状态
    private String intention;//用户意向
    private String firmTime;//返回具体时间
    private int result;//通话结果
}
