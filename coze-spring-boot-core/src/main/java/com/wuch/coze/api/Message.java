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
     * 会话id
     */
    private String conversationId = ChatClient.DEFAULT_CONVERSATION_ID;

    /**
     * 对话id
     */
    private String chatId;

    /**
     * 消息内容
     */
    private String text;

    /**
     * 元数据，可以在返回中获取到
     */
    private Map<String, String> metaData;


}
