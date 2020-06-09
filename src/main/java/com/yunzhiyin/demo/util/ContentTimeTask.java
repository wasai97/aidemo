package com.yunzhiyin.demo.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yunzhiyin.demo.mapper.ContentMapper;
import com.yunzhiyin.demo.mapper.PhoneRecordMapper;
import com.yunzhiyin.demo.mapper.RadioMapper;
import com.yunzhiyin.demo.pojo.Content;
import com.yunzhiyin.demo.pojo.PhoneRecord;
import com.yunzhiyin.demo.pojo.Radio;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时扫描数据库，存储通话内容
 */
@Component
@Slf4j
public class ContentTimeTask {
    @Resource
    private PhoneRecordMapper phoneRecordMapper;

    @Resource
    private RadioMapper radioMapper;

    @Resource
    private ContentMapper contentMapper;

    @Resource
    private RestTemplate restTemplate;

    @Value("${CheckStateURL}")
    private String CheckStateURL;

    @Value("${CheckContentURL}")
    private String CheckContentURL;

    @Value("${CheckRadioURL}")
    private String CheckRadioURL;

    @Value("${fastDFURL}")
    private String fastDFURL;

    @Scheduled(cron="0/10 * *  * * ? ")
    public void saveContent(){
        /**
         * 步骤：1.首先扫描没有记录的
         *       2 得到一个list集合 存放的没有聊天内容的记录
         *       3.遍历集合，判断对应的任务是否结束
         */
        List<PhoneRecord> noContentList = phoneRecordMapper.getNoContent();
        log.info("********content没有聊天内容的记录***********"+noContentList);
        List<PhoneRecord> completePhoneRecords = new ArrayList<>();
        for (PhoneRecord phoneRecord : noContentList) {
            String dutyId = phoneRecord.getDutyId();
            String phoneNumber = phoneRecord.getPhoneNumber();
            Integer id = phoneRecord.getId();
            Boolean isEnd = dutyIsEnd(dutyId);
            String firmTime = null;
            if (isEnd){
                Map<String, Object> stringObjectMap = saveContent(dutyId, phoneNumber, id);
                String intention = (String) stringObjectMap.get("intention");
                int result = (int) stringObjectMap.get("result");
                log.info("**********电话号码："+phoneNumber+"**********intention:"+intention);
                if ("返回投递时间".equals(intention)){
                    firmTime = getFirmTime(dutyId, phoneNumber, id, intention);
                }
                phoneRecord.setHaveResult(1);
                phoneRecord.setIntention(intention);
                phoneRecord.setFirmTime(firmTime);
                phoneRecord.setResult(result);
                completePhoneRecords.add(phoneRecord);
            }
        }
        if (completePhoneRecords.size() != 0){
            phoneRecordMapper.updateHaveResult(completePhoneRecords);
            log.info("****************批量修改内容完成");
        }
    }


    @Scheduled(cron="0/17 * *  * * ? ")
    public void saveRadio(){
        List<PhoneRecord> noRadioList = phoneRecordMapper.getNoRadio();
        log.info("********radio没有音频的记录***********"+noRadioList);
        List<PhoneRecord> completePhoneRecords = new ArrayList<>();
        for (PhoneRecord phoneRecord : noRadioList) {
            String dutyId = phoneRecord.getDutyId();
            String phoneNumber = phoneRecord.getPhoneNumber();
            Integer id = phoneRecord.getId();
            Boolean isEnd = dutyIsEnd(dutyId);
            if (isEnd){
                saveRadio(dutyId,phoneNumber,id);
                phoneRecord.setHaveRadio(1);
                completePhoneRecords.add(phoneRecord);
            }

        }
        if (completePhoneRecords.size() != 0){
            phoneRecordMapper.updateHaveRadio(completePhoneRecords);
            log.info("***********批量修改音频完成***********");
        }
    }

    /**
     * 定时修改记录状态
     */
    @Scheduled(cron="0/9 * *  * * ? ")
    public void checkState(){
        List<PhoneRecord> updataState = phoneRecordMapper.getUpdataState();
        log.info("****************查看还未完成的记录:"+updataState);
        List<PhoneRecord> completeState = new ArrayList<>();
        for (PhoneRecord phoneRecord : updataState) {
            String dutyId = phoneRecord.getDutyId();
            int state = getPhoneRecordState(dutyId);
            if (state == 3 || state == 5){
                state = 4;
            }
            phoneRecord.setState(state);
            completeState.add(phoneRecord);
        }
        if (completeState.size() != 0){
            phoneRecordMapper.updateState(completeState);
            log.info("*****************批量修改状态完成*********************");
        }


    }

    /**
     * 判断任务是否结束
     * @param dutyId
     * @return
     * 0为未执行，1为执行中，2为
     * 暂停，3为逻辑删除，4为执行
     * 完毕，5为物理删除
     */
    public Boolean dutyIsEnd(String dutyId){
        JSONObject forObject = restTemplate.getForObject(CheckStateURL+dutyId, JSONObject.class);
        log.info("************"+forObject);
        Integer result = (Integer) forObject.get("value");
        if (result == 4){
            return true;
        }
        return false;
    }



    /**
     * 保存查询音频用的callId
     * @param dutyId
     */
    public void saveRadio(String dutyId,String phoneNumber,int id){
        String tempPath = null;
        try {
            tempPath = ResourceUtils.getFile("radio").getAbsolutePath()+"/";
        } catch (FileNotFoundException e) {
            log.info("*************************保存音频时获取项目地址失败");
            e.printStackTrace();
        }
        JSONObject content = restTemplate.getForObject(CheckContentURL+dutyId, JSONObject.class);
        log.info("*************开始保存radio***************");
        JSONObject value = content.getJSONObject("value");
        String isBM = "true";
        if (value != null){
            ArrayList<Object> dutyResources = (ArrayList) value.get("dutyResources");
            //用来存储Radio 保存callId，用来获取音频
            ArrayList<Radio> radios = new ArrayList<>();
            log.info("*************************dutyResources size:"+dutyResources.size());
            for (int i = 0; i < dutyResources.size(); i++) {
                Map<Object,Object> dutyResourcesMap = (Map<Object, Object>) dutyResources.get(i);
                String callId = (String) dutyResourcesMap.get("callId");
                //线上加0
                String phoneNumber1 = (String) dutyResourcesMap.get("phoneNumber");
                int id1 = (int) dutyResourcesMap.get("id");
                int callNumber = (int) dutyResourcesMap.get("callNumber");
                if (phoneNumber.equals(phoneNumber1)){
                    Radio radio = new Radio();
                    radio.setCallId(callId);
                    radio.setDutyId(dutyId);
                    radio.setId(id1);
                    radio.setPhoneNumber(phoneNumber1);
                    radio.setCallNumber(callNumber);
                    HttpHeaders headers = new HttpHeaders();
                    String url = CheckRadioURL+"callId="+callId+"&"+"isBM="+isBM;
                    ResponseEntity<byte[]> entity = restTemplate.exchange(url, HttpMethod.GET,new HttpEntity<>(headers),byte[].class);
                    log.info("********************entity"+entity.getBody());
                    byte[] body = entity.getBody();
                    Utils.downRadio(body,callId);

                    AudioConvert.convert(tempPath+callId+".wav",
                            tempPath+callId+".mp3", avcodec.AV_CODEC_ID_MP3,
                            8000, 16,1);

                    //将MP3文件上传之fast服务器
                    HttpHeaders httpHeaders = new HttpHeaders();
                    MediaType type = MediaType.parseMediaType("multipart/form-data");
                    httpHeaders.setContentType(type);
                    //设置请求体
                    FileSystemResource fileSystemResource = new FileSystemResource(tempPath+callId+".mp3");
                    MultiValueMap<String,Object> form = new LinkedMultiValueMap<>();
                    form.add("file",fileSystemResource);
                    form.add("filename",callId+".mp3");
                    //用HttpEntity封装整个请求报文
                    HttpEntity<MultiValueMap<String,Object>> files = new HttpEntity<>(form,headers);
                    String radioPath = restTemplate.postForObject(fastDFURL,files,String.class);
                    log.info("*******************************radioPath"+radioPath);
                    JSONObject jsonRadioPath = JSONObject.parseObject(radioPath);
                    JSONObject data = jsonRadioPath.getJSONObject("data");
                    String radioPathUrl = data.getString("url");
                    log.info("*****************************radioUrl:"+url);
                    radio.setRadioAddress(radioPathUrl);
                    radios.add(radio);
                }
            }
            radioMapper.insert(radios);
            log.info("************************保存音频完毕*****************");
        }
    }

    /**
     * 存储通话内容到本地
     * @param dutyId
     */
    public Map<String,Object> saveContent(String dutyId,String phoneNumber,Integer id){
        log.info("********************保存内容****************");
        Map<String,Object> resultMap = new HashMap<>();
        JSONObject contentJSONObject = restTemplate.getForObject(CheckContentURL+dutyId, JSONObject.class);
        JSONObject value = contentJSONObject.getJSONObject("value");
        String intention = null;
        int result = 0;
        if (value != null){
            ArrayList<Object> dutyResources = (ArrayList) value.get("dutyResources");
            for (int i = 0; i < dutyResources.size(); i++) {
                Map<Object,Object> dutyResourcesMap = (Map<Object, Object>) dutyResources.get(i);
                String intentions = (String) dutyResourcesMap.get("intentions");
                //log.info("*********************intention:"+intentions);
                //上线加0
                String phoneNumber1 = (String) dutyResourcesMap.get("phoneNumber");
                int id1 = (int) dutyResourcesMap.get("id");
                Integer dutyId1 = (Integer) dutyResourcesMap.get("dutyId");
                int result1 = (int) dutyResourcesMap.get("result");
                if (Integer.parseInt(dutyId) == dutyId1 && id == id1 && phoneNumber.equals(phoneNumber1)){
                    intention = intentions;
                    result = result1;
                    break;
                }
            }
        }
        resultMap.put("intention",intention);
        resultMap.put("result",result);
        String contentString = contentJSONObject.toJSONString();
        Content content = new Content();
        content.setDutyId(dutyId);
        content.setMessage(contentString);
        content.setPhoneNumber(phoneNumber);
        contentMapper.insert(content);
        log.info("*******************保存内容完毕*****************");
        return resultMap;

    }

    /**
     * 查看任务状态
     * @param dutyId
     * @return
     */
    public int getPhoneRecordState(String dutyId){
        JSONObject forObject = restTemplate.getForObject(CheckStateURL+dutyId, JSONObject.class);
        log.info("************"+forObject);
        Integer result = (Integer) forObject.get("value");
        return result;
    }

    public String getFirmTime(String dutyId,String phoneNumber,int id,String intention){
        Content detailContent = contentMapper.getDetailContent(dutyId,phoneNumber);
        String message = detailContent.getMessage();
        JSONObject jsonObject = JSONObject.parseObject(message);
        JSONObject value = jsonObject.getJSONObject("value");
        JSONArray records = (JSONArray) value.get("records");
        for (int i = 0; i < records.size(); i++) {
            JSONObject record = (JSONObject) records.get(i);
            String execId1 = record.getString("execId");
            String dutyId1 = record.getString("dutyId");
            String intention1 = record.getString("intention");
            String flowResult = record.getString("flowResult");
            if (dutyId.equals(dutyId1) && id == Integer.parseInt(execId1) && intention.equals(intention1)){
                return flowResult;
            }
        }
        return null;
    }
}
