package com.yunzhiyin.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Duty {
    private int flow_id;
    private String type_name;
    private String duty_name;
    private String rabbit_running_time;
    private String duty_start_period;
    private String duty_end_period;
    private String create_user;
    private String create_user_id;
    private int duplication;
    private int cycle_mark;
    private int period;
    private String phone_num;
    private String priority;
    private String dataPermissions;
    private String repStrategy;

}
