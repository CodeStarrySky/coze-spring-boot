package com.wuch.coze.api;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.SubmitToolOutputsReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatToolCall;
import com.coze.openapi.client.chat.model.ToolOutput;
import com.coze.openapi.client.connversations.ClearConversationReq;
import com.coze.openapi.client.connversations.CreateConversationReq;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.service.CozeAPI;
import com.wuch.coze.memory.DataMemory;
import com.wuch.coze.util.TypeResolverHelper;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.coze.openapi.client.chat.model.ChatEventType.*;

@RequiredArgsConstructor
public class ChatClient{

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private static final String REDIS_PREKEY = "chat:user:";

    private final ApplicationContext applicationContext;

    private final DefaultListableBeanFactory beanFactory;

    private final CozeAPI coze;

    private final DataMemory dataMemory;

    private final CozeProperties properties;

    public static final String DEFAULT_USER_ID = "SYSTEM_DEFAULT_USER";

    public static final String DEFAULT_CHAT_ID = "DEFAULT_CHAT_ID";

    public Flux<String> chat(com.wuch.coze.api.Message message) {
        return chat(message.getUserId(), message.getChatId(), message.getText(), message.getMetaData());
    }

    public Flux<String> chat(Long userId, String message) {
        return chat(String.valueOf(userId), message);
    }

    public Flux<String> chat(String userId, String message) {
        return chat(userId, DEFAULT_CHAT_ID, message, null);
    }

    public Flux<String> chat(String userId, String message, Map<String, String> metaData) {
        return chat(userId, DEFAULT_CHAT_ID, message, metaData);
    }

    /**
     * 使用默认用户id聊天
     * @param message 消息
     * @return 响应
     */
    public Flux<String> chat(String message) {
        return chat(DEFAULT_USER_ID, DEFAULT_CHAT_ID, message, null);
    }

    public Flux<String> chat(String userId, String chatId, String message, Map<String, String> metaData) {
        return chat(userId, chatId, Message.buildUserQuestionText(message), metaData);
    }

    public Flux<String> chat(String userId, String chatId, Message message, Map<String, String> metaData) {
        return chat(userId, chatId, Collections.singletonList(message), metaData);
    }


    public Flux<String> chat(String userId, String chatId, List<Message> messages, Map<String, String> metaData) {
        String conversationId = conversation(userId, chatId);
        Map<String, String> defaultMetaData = new HashMap<>();
        defaultMetaData.put("userId", userId);
        if (metaData != null) {
            defaultMetaData.putAll(metaData);
        }
        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(properties.getBotId())
                        .userID(userId)
                        .conversationID(conversationId)
                        .readTimeout(properties.getReadTimeout())
                        .connectTimeout(properties.getConnectTimeout())
                        .messages(messages)
                        .metaData(defaultMetaData)
                        .autoSaveHistory(properties.getAutoSaveHistory())
                        .build();


        Flowable<ChatEvent> resp = coze.chat().stream(req);

        return Flux.from(resp).flatMap(this::process).handle(this::content);
    }
    public String conversation(String userId, String chatId) {
        String conversationId = null;
        if (StringUtils.isNotBlank(userId) || StringUtils.isNotBlank(chatId)) {
            conversationId = dataMemory.get(getConversationIdKey(userId, chatId));
        }
        if (StringUtils.isBlank(conversationId)) {
            log.info("用户【{}】进入聊天", userId);
            conversationId = createConversation(userId, chatId);
        }
        return conversationId;
    }

    public String createConversation(String userId, String chatId) {
        String message = "当前对话的用户id(userId)是: " + userId + ",用户信息（userName)是：" +  "WUCH" + "当前日期是: " + LocalDate.now() + ";" ;
        CreateConversationReq build = CreateConversationReq.builder().botID(properties.getBotId())
                .messages(Collections.singletonList(Message.buildUserQuestionText(message))).build();
        String conversationId = coze.conversations().create(build).getConversation().getId();
        dataMemory.add(getConversationIdKey(userId, chatId), conversationId);
        return conversationId;
    }

    private String getConversationIdKey(String userId, String chatId) {
        return REDIS_PREKEY + userId + ":" + chatId;
    }

    public void clearConversation(String userId, String chatId, String conversationId) {
        dataMemory.clear(getConversationIdKey(userId, chatId));
        ClearConversationReq req = ClearConversationReq.builder().conversationID(conversationId).build();
        coze.conversations().clear(req);
    }

    private Flux<ChatEvent> process(ChatEvent event) {
        if (CONVERSATION_CHAT_REQUIRES_ACTION.equals(event.getEvent())) {
            return submitToolOutputs(event);
        }
        return Flux.just(event);
    }

    private void content(ChatEvent event, SynchronousSink<String> sink) {
        if (CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
            sink.next(event.getMessage().getContent());
        } else if (CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
            sink.complete();
        } else if (CONVERSATION_CHAT_FAILED.equals(event.getEvent()) || ERROR.equals(event.getEvent())) {
            sink.next("很抱歉未能找到您想要的答案，请重新提问！");
            log.error("聊天失败message：{}, lasetError: {}", event.getMessage(), event.getChat().getLastError());
        }
    }

    private Flux<ChatEvent> submitToolOutputs(ChatEvent pluginEvent) {
        List<ToolOutput> toolOutputs = new ArrayList<>();
        for (ChatToolCall callInfo :
                pluginEvent.getChat().getRequiredAction().getSubmitToolOutputs().getToolCalls()) {
            String callId = callInfo.getID();
            String functionName = callInfo.getFunction().getName();
            String jsonData = callInfo.getFunction().getArguments();
            Object result = null;
            try {
                if(applicationContext.containsBean(functionName)) {
                    Object bean = applicationContext.getBean(functionName);
                    if (bean instanceof Function<?, ?> function) {
                        ResolvableType functionType = TypeResolverHelper.resolveBeanType((GenericApplicationContext) applicationContext, functionName);
                        ResolvableType functionInputType = TypeResolverHelper.getFunctionArgumentType(functionType, 0);
                        Type type = functionInputType.getType();
                        result = function.apply(JSONObject.parseObject(jsonData, type));
                    } else if (bean instanceof BiFunction) {
                        Map<String, String> metaData = pluginEvent.getChat().getMetaData();
                        @SuppressWarnings("unchecked")
                        BiFunction<String, Map<String, String>, String> call = (BiFunction<String, Map<String, String>, String>) bean;
                        result = call.apply(jsonData, metaData);
                    } else {
                        result = "没有找到可用的端插件执行";
                    }
                } else {
                    result = "没有找到对应的端插件执行";
                }

            } catch (Exception e) {
                result = "执行工具的时候出现异常，请给用户非常友好的提示！";
                log.error("端插件执行异常：", e);
            }
            toolOutputs.add(ToolOutput.of(callId, processResult(result)));
        }
        SubmitToolOutputsReq toolReq =
                SubmitToolOutputsReq.builder()
                        .chatID(pluginEvent.getChat().getID())
                        .conversationID(pluginEvent.getChat().getConversationID())
                        .toolOutputs(toolOutputs)
                        .readTimeout(properties.getReadTimeout())
                        .connectTimeout(properties.getConnectTimeout())
                        .build();

        Flowable<ChatEvent> flow = coze.chat().streamSubmitToolOutputs(toolReq);
        return Flux.from(flow).flatMap(this::process);
    }


    /**
     * 如果返回结果是字符串，则判断是否是json格式，如果是json格式，则直接返回，否则包装成AiResult对象返回，此时coze请用 message 接收
     * 是对象就转为json字符串
     * @param obj 结果对象
     * @return json
     */
    private String processResult(Object obj) {
        if (obj instanceof String str) {
            if (JSON.isValid(str)) {
                return str;
            }
            obj = AiResult.onlyMessage(str);
        }
        return JSONObject.toJSONString(obj);
    }

}
