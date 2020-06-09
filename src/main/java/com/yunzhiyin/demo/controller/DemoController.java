package com.yunzhiyin.demo.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunzhiyin.demo.mapper.ContentMapper;
import com.yunzhiyin.demo.mapper.PhoneRecordMapper;
import com.yunzhiyin.demo.mapper.RadioMapper;
import com.yunzhiyin.demo.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.yunzhiyin.demo.util.Utils.params;


@Controller
@Slf4j
public class DemoController {
    @Autowired
    private RestTemplate restTemplate;

    @Resource
    private PhoneRecordMapper phoneRecordMapper;

    @Resource
    private RadioMapper radioMapper;

    @Resource
    private ContentMapper contentMapper;

    @Value("${CallURL}")
    private String CallURL;

    @Value("${CheckStateURL}")
    private String CheckStateURL;

    @Value("${CheckContentURL}")
    private String CheckContentURL;


    @Value("${CheckRadioURL}")
    private String CheckRadioURL;
    /**
     * 拨打电话接口
     * @param caller
     * @return
     * @throws ParseException
     */
    @ResponseBody
    @RequestMapping("/phone")
    public CommonResult<String> callPhone(@RequestBody Caller caller) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<String> phoneNumbers = caller.getPhoneNumbers();
        Dutys dutys = params(caller);
        String s = JSONObject.toJSONString(dutys);
        log.info("****************拨打电话传入的参数："+s);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("x-auth-token", "token内容");

        //用来存储一次呼叫多个电话的号码，方便用于存进数据库
        List<PhoneRecord> phoneRecordList = new ArrayList<>();
        //拨打电话
        JSONObject jsonObject = restTemplate.postForObject(CallURL,  new HttpEntity<Object>(s, headers), JSONObject.class);
        log.info("************************jsonObject:"+jsonObject);
        Boolean isSuccess = (Boolean) jsonObject.get("success");
        log.info("*************是否拨打成功："+isSuccess);
        //失败的描述信息
        String message = (String) jsonObject.get("message");
        if (isSuccess == false){
            //拨打失败的dutyId统一为 0
            String dutyId = "0";
            for (String phoneNumber : phoneNumbers) {
                //拨号失败的记录
                PhoneRecord failPhoneRecord = new PhoneRecord();
                failPhoneRecord.setDutyId(dutyId);
                failPhoneRecord.setId(null);
                failPhoneRecord.setCallDate(simpleDateFormat.format(new Date()));
                failPhoneRecord.setUserName(caller.getName());
                failPhoneRecord.setIsSuccess(0);
                failPhoneRecord.setPhoneNumber(0+phoneNumber);
                failPhoneRecord.setHaveResult(0);
                failPhoneRecord.setState(0);
                phoneRecordList.add(failPhoneRecord);
            }
            //拨号失败入库
            phoneRecordMapper.insert(phoneRecordList);
            return new CommonResult<>(400,message,null);
        }
        //拨打成功之后才会有value
        JSONObject value = jsonObject.getJSONObject("value");
        Set<String> strings = value.keySet();
        //获取的每次拨电话的唯一任务id
        String dutyId = (String) strings.toArray()[0];
        log.info("******************每次拨通电话的唯一任务id："+dutyId);
        List<Map<String, Object>> detailPhone = (List<Map<String, Object>>) value.get(dutyId);
        for (Map<String, Object> phoneMaps : detailPhone) {
            Object id = phoneMaps.get("id");
            log.info("*****************每个电话对应的id："+id);
            Object phoneNumber = phoneMaps.get("phoneNumber");
            log.info("**************具体的电话号码："+phoneNumber);
            log.info("******************************");
            PhoneRecord phoneRecord = new PhoneRecord();
            phoneRecord.setDutyId(dutyId);
            phoneRecord.setId((Integer) id);
            //线上加0
            phoneRecord.setPhoneNumber((String) phoneNumber);
            String date = simpleDateFormat.format(new Date());
            phoneRecord.setCallDate(date);
            phoneRecord.setUserName(caller.getName());
            phoneRecord.setIsSuccess(1);
            phoneRecord.setHaveResult(0);
            phoneRecord.setHaveRadio(0);
            phoneRecord.setState(1);
            phoneRecordList.add(phoneRecord);
        }
        phoneRecordMapper.insert(phoneRecordList);

        return new CommonResult<>(200,"拨打成功",null);
    }

    /**
     * 获取某个用户的历史纪录
     * @param userName
     * @return
     */
    @ResponseBody
    @RequestMapping("/history")
    public CommonResult<List<PhoneRecord>> getHistory(@RequestParam("userName") String userName){
        log.info("***********查看历史记录入参userName**********"+userName);
        try{
            List<PhoneRecord> historyByUserName = phoneRecordMapper.getHistoryByUserName(userName);
            return new CommonResult<>(200,"success",historyByUserName);
        }catch (Exception e){
           e.printStackTrace();
            return new CommonResult<>(200,e.getMessage(),null);
        }

    }

    /**
     * 0为未执行，1为执行中，2为
     * 暂停，4为执行完毕，
     * @param dutyId
     * @param phoneNumber
     * @param execId
     * @return
     */
    @RequestMapping("/detailContent")
    @ResponseBody
    public CommonResult getContent(@RequestParam("dutyId") String dutyId,@RequestParam("phoneNumber") String phoneNumber,@RequestParam("execId") String execId){
        log.info("*****查看聊天内容入参********dutyID="+dutyId+"********phoneNumber="+phoneNumber+"**********execId="+execId);
        //查询任务是否完成
        JSONObject forObject = restTemplate.getForObject(CheckStateURL+dutyId, JSONObject.class);
        Integer isEnd = (Integer) forObject.get("value");
        String messageState = forObject.getString("message");
        if (isEnd == 4){
            try{
                Content detailContent = contentMapper.getDetailContent(dutyId,phoneNumber);
                String message = detailContent.getMessage();
                JSONObject jsonObject = JSONObject.parseObject(message);
                //创建一个List用来存放需要返回的结果
                List<Map<String,String>> resultContents = new ArrayList<>();
                log.info("**************************************"+jsonObject);
                JSONObject value = jsonObject.getJSONObject("value");

                JSONArray records = (JSONArray) value.get("records");
                for (int i = 0; i < records.size(); i++) {
                    JSONObject record = (JSONObject) records.get(i);
                    String execId1 = record.getString("execId");
                    if (!execId1.equals(execId)){
                        continue;
                    }
                    String playContentOne = record.getString("playContent");
                    String[] split = playContentOne.split("=");
                    String playContent = split[split.length-1];
                    String flowResult = record.getString("flowResult");
                    String endPlay = record.getString("endPlay");
                    Map<String,String> mapResult = new HashMap<>();
                    mapResult.put("playContent",playContent);
                    mapResult.put("flowResult",flowResult);
                    mapResult.put("endPlay",endPlay);
                    resultContents.add(mapResult);
                }
                CommonResult<List<Map<String,String>>> result = new CommonResult<>(200,"success",resultContents);
                return result;
            }catch (Exception e){
                e.printStackTrace();
                CommonResult<List<Map<String,String>>> result = new CommonResult<>(200,e.getMessage(),null);
                return result;
            }
        }else {
            return new CommonResult(400,messageState,null);
        }

    }


    @RequestMapping("/radio")
    @ResponseBody
    public CommonResult getRadio(@RequestParam("dutyId") String dutyId,@RequestParam("phoneNumber") String phoneNumber,@RequestParam("id") String id){
        log.info("*****dutyId="+dutyId+"*****phoneNumber="+phoneNumber+"***********id="+id);
        String radioUrl = radioMapper.getRadio(dutyId, phoneNumber,id);
        log.info("*******************radioUrl"+radioUrl);
        CommonResult<String> result = new CommonResult();
        result.setCode(200);
        result.setMessage("success");
        result.setData(radioUrl);
        return result;
    }







}
