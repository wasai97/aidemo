package com.yunzhiyin.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 存储聊天内容
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private String dutyId;
    private String message;
    private String phoneNumber;
    private String id;
}
