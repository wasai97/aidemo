package com.yunzhiyin.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Radio {
    private String dutyId;
    private String callId;
    private String phoneNumber;
    private String radioAddress;//音频地址
    private int id;
    private int callNumber;
}
