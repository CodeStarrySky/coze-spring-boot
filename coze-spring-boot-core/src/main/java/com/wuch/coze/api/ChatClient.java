package com.wuch.coze.api;

import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.SubmitToolOutputsReq;
import com.coze.openapi.client.chat.model.ChatEvent;
import com.coze.openapi.client.chat.model.ChatEventType;
import com.coze.openapi.client.chat.model.ChatToolCall;
import com.coze.openapi.client.chat.model.ToolOutput;
import com.coze.openapi.client.connversations.ClearConversationReq;
import com.coze.openapi.client.connversations.CreateConversationReq;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.service.CozeAPI;
import com.wuch.coze.toolcall.BaseToolCall;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.time.LocalDate;
import java.util.*;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class ChatClient{

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private static final String REDIS_PREKEY = "chat:user:";

    private final ApplicationContext applicationContext;

    private final CozeAPI coze;

    private final StringRedisTemplate stringRedisTemplate;

    private final CozeProperties properties;

    public Flux<String> chat(Long userId, String message) {
        return chat(userId, message, null);
    }


    public Flux<String> chat(Long userId, String message, Map<String, String> metaData) {
        String conversationId = conversation(userId);
        log.info("用户【{}】：{}", userId, message);
        Map<String, String> defaultMetaData = new HashMap<>();
        defaultMetaData.put("userId", userId + "");
        if (metaData != null) {
            defaultMetaData.putAll(metaData);
        }
        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(properties.getBotId())
                        .userID(userId + "")
                        .conversationID(conversationId)
                        .readTimeout(properties.getReadTimeout())
                        .connectTimeout(properties.getConnectTimeout())
                        .messages(Collections.singletonList(Message.buildUserQuestionText(message)))
                        .metaData(defaultMetaData)
                        .autoSaveHistory(true)
                        .build();


        Flowable<ChatEvent> resp = coze.chat().stream(req);

        return Flux.from(resp).handle(this::content);
    }

    public String conversation(Long userId) {
        String conversationId = stringRedisTemplate.opsForValue().get(REDIS_PREKEY + userId);
        if (StringUtils.isBlank(conversationId)) {
            log.info("用户【{}】进入聊天", userId);
            conversationId = createConversation(userId);
        }
        return conversationId;
    }

    public String createConversation(Long userId) {

        String message = "当前对话的用户id(userId)是: " + userId + ",用户信息（userName)是：" +  "WUCH" + "当前日期是: " + LocalDate.now() + ";" ;
        CreateConversationReq build = CreateConversationReq.builder().botID(properties.getBotId())
                .messages(Collections.singletonList(Message.buildUserQuestionText(message))).build();
        String conversationId = coze.conversations().create(build).getConversation().getId();
        stringRedisTemplate.opsForValue().set(REDIS_PREKEY + userId, conversationId);
        return conversationId;
    }

    public void clearConversation(Long userId, String conversationId) {
        stringRedisTemplate.delete(REDIS_PREKEY + userId);
        ClearConversationReq req = ClearConversationReq.builder().conversationID(conversationId).build();
        coze.conversations().clear(req);
    }

    private void content(ChatEvent event, SynchronousSink<String> sink) {
        if (ChatEventType.CONVERSATION_MESSAGE_DELTA.equals(event.getEvent())) {
            sink.next(event.getMessage().getContent());
        }
        if (ChatEventType.CONVERSATION_CHAT_REQUIRES_ACTION.equals(event.getEvent())) {
            submitToolOutputs(event.getChat().getConversationID(), event, sink);
        }
        if (ChatEventType.CONVERSATION_CHAT_COMPLETED.equals(event.getEvent())) {
            sink.complete();
        }
        if (ChatEventType.CONVERSATION_CHAT_FAILED.equals(event.getEvent()) || ChatEventType.ERROR.equals(event.getEvent())) {
            sink.next("很抱歉未能找到您想要的答案，请重新提问！");
            log.error("聊天失败message：{}, lasetError: {}", event.getMessage(), event.getChat().getLastError());
        }
    }

    private void submitToolOutputs(String conversationId, ChatEvent pluginEvent, SynchronousSink<String> sink) {
        List<ToolOutput> toolOutputs = new ArrayList<>();
        for (ChatToolCall callInfo :
                pluginEvent.getChat().getRequiredAction().getSubmitToolOutputs().getToolCalls()) {
            String callId = callInfo.getID();
            String functionName = callInfo.getFunction().getName();
            String jsonData = callInfo.getFunction().getArguments();
            try {
                if(applicationContext.containsBean(functionName)) {
                    Object bean = applicationContext.getBean(functionName);
                    if (bean instanceof BaseToolCall) {
                        Map<String, String> metaData = pluginEvent.getChat().getMetaData();
                        @SuppressWarnings("unchecked")
                        BiFunction<Map<String, String>, String, String> call = (BaseToolCall<String>) bean;
                        String result = call.apply(metaData, jsonData);
                        toolOutputs.add(ToolOutput.of(callId, result));
                    } else {
                        toolOutputs.add(ToolOutput.of(callId, AiResult.fail("没有找到对应的端插件执行").toJson()));
                    }
                } else {
                    toolOutputs.add(ToolOutput.of(callId, AiResult.fail("没有找到对应的端插件执行").toJson()));
                }
            } catch (Exception e) {
                toolOutputs.add(ToolOutput.of(callId, AiResult.fail("执行工具的时候出现异常，请给用户非常友好的提示！").toJson()));
                log.error("端插件执行异常：", e);
            }


        }

        SubmitToolOutputsReq toolReq =
                SubmitToolOutputsReq.builder()
                        .chatID(pluginEvent.getChat().getID())
                        .conversationID(conversationId)
                        .toolOutputs(toolOutputs)
                        .readTimeout(properties.getReadTimeout())
                        .connectTimeout(properties.getConnectTimeout())
                        .build();

        Flowable<ChatEvent> flow = coze.chat().streamSubmitToolOutputs(toolReq);
        flow.blockingForEach(event -> content(event, sink));
    }


}
