package com.yunzhiyin.demo.util;


import com.yunzhiyin.demo.pojo.Caller;
import com.yunzhiyin.demo.pojo.Duty;
import com.yunzhiyin.demo.pojo.DutyResource;
import com.yunzhiyin.demo.pojo.Dutys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
public class Utils {

    @Resource
    private static RestTemplate restTemplate;

    @Value("${CheckStateURL}")
    private static String CheckStateURL;
    /**
     * 组装参数
     * @param caller
     * @return
     */
    public static Dutys params(Caller caller){
        Dutys dutys = new Dutys();
        Duty duty = new Duty();
        duty.setFlow_id(18);
        duty.setType_name("test");
        String duty_name = UUID.randomUUID().toString().replaceAll("-","").substring(0,5);
        duty.setDuty_name(duty_name);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date duty_start_period = new Date();
        String format = simpleDateFormat.format(duty_start_period);
        Date duty_end_period = new Date();
        String format1 = simpleDateFormat.format(duty_end_period);
        duty.setDuty_start_period(format);
        duty.setDuty_end_period(format1);
        duty.setCreate_user("外呼系统");
        duty.setCreate_user_id("646f5bb2-0e2e-4910-a5d7-dc9dd73ecde3");
        duty.setDuplication(0);
        duty.setCycle_mark(0);
        duty.setPeriod(0);
        duty.setPhone_num("10000");
        duty.setPriority("0");
        duty.setDataPermissions("1000");
        duty.setRabbit_running_time("{\"showName\":\"机器时间\",\"rrt\":[\"09: 00~16: 35\",\"15: 55~23: 30\"]}");
        List<String> phoneNumbers = caller.getPhoneNumbers();
        DutyResource[] dutyResources = new DutyResource[phoneNumbers.size()];
        for (int i = 0; i < phoneNumbers.size(); i++) {
            String phoneNumber = phoneNumbers.get(i);
            DutyResource dutyResource = new DutyResource();
            //线上去0
            dutyResource.setPhone_number(0+phoneNumber);
            dutyResource.setUser_info("{\"手机号\":\"17764402061\",\"地址\":\"蜀山区中国声谷\",\"来自城市\":\"合肥\"}");
            dutyResources[i] = dutyResource;
        }
        dutys.setDuty(duty);
        dutys.setDutyResources(dutyResources);
        return dutys;
    }

    /**
     * 下载音频文件
     * @param radio
     * @throws Exception
     * Files.write(Paths.get(silkPath), bytes, StandardOpenOption.CREATE);
     *
     */
    public static void downRadio(byte[] radio,String callId) {
        log.info("******************开始下载音频***********************");
        String tempPath = null;
        try {
            tempPath = ResourceUtils.getFile("radio").getAbsolutePath()+"/";
            log.info("*********************tempPath"+tempPath);
        } catch (FileNotFoundException e) {
            log.info("*************************获取项目地址失败");
            e.printStackTrace();
        }

        String fileName = callId+".wav";
        File file = new File(tempPath,fileName);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int len = 0;
        byte[] bytes = new byte[1024];
        try (InputStream inputStream = new ByteArrayInputStream(radio);
        FileOutputStream outputStream = new FileOutputStream(file);){

            while (((len = inputStream.read(bytes)) != -1)){
                outputStream.write(bytes,0,len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
