package com.yunzhiyin.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接受前端传过来的电话号码和用户名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caller {
    private List<String> phoneNumbers;
    private String name;
}
