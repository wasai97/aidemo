package com.yunzhiyin.demo.pojo;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dutys {
    private Duty duty;
    private DutyResource[] dutyResources;

}
