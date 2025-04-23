package com.wuch.coze.api;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class AiResult {

    private String status;
    private String message;

    private Object data;

    public static AiResult success(Object data) {
        AiResult result = new AiResult();
        result.setStatus("success");
        result.setMessage("处理成功！");
        result.setData(data);
        return result;
    }

    public static AiResult fail(String msg) {
        AiResult result = new AiResult();
        result.setStatus("false");
        result.setMessage(msg);
        result.setData(null);
        return result;
    }

    public static AiResult onlyMessage(String msg) {
        AiResult result = new AiResult();
        result.setMessage(msg);
        return result;
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

}
