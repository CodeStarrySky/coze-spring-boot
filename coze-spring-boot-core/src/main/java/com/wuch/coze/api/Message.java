package com.wuch.coze.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    /**
     * 用户id
     */
    private String userId = ChatClient.DEFAULT_USER_ID;

    /**
     * 聊天id
     */
    private String chatId = ChatClient.DEFAULT_CHAT_ID;

    /**
     * 消息内容
     */
    private String text;

    /**
     * 元数据，可以在返回中获取到
     */
    private Map<String, String> metaData;


}
