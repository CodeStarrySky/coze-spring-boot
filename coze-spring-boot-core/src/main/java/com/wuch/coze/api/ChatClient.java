package com.wuch.coze.api;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.coze.openapi.client.audio.transcriptions.CreateTranscriptionsReq;
import com.coze.openapi.client.audio.transcriptions.CreateTranscriptionsResp;
import com.coze.openapi.client.chat.CancelChatReq;
import com.coze.openapi.client.chat.CreateChatReq;
import com.coze.openapi.client.chat.SubmitToolOutputsReq;
import com.coze.openapi.client.chat.model.Chat;
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
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.coze.openapi.client.chat.model.ChatEventType.*;

@RequiredArgsConstructor
public class ChatClient{

    private static final Logger log = LoggerFactory.getLogger(ChatClient.class);

    private static final String CONVERSATION_PREFIX = "coze:conversation:";

    private static final String CHAT_PREFIX = "coze:conversation:chat:";

    public static final String LOCAL_CHAT_ID = "localChatId";

    public static final String LOCAL_CONVERSATION_ID = "localConversationId";

    public static final String LOCAL_USER_ID = "localUserId";

    private final ApplicationContext applicationContext;


    private final CozeAPI coze;

    private final DataMemory dataMemory;

    private final CozeProperties properties;

    public static final String DEFAULT_USER_ID = "SYSTEM_DEFAULT_USER";

    public static final String DEFAULT_CONVERSATION_ID = "SYSTEM_DEFAULT_CONVERSATION_ID";

    public Flux<String> chat (String chatId, String message) {
        return chat(DEFAULT_CONVERSATION_ID, chatId, message);
    }

    public Flux<String> chat(String conversationId, String chatId, String message) {
        return chat(DEFAULT_USER_ID, conversationId, chatId, Message.buildUserQuestionText(message));
    }

    public Flux<String> chat(String conversationId, String chatId, String message, Map<String, String> metaData) {
        return chat(DEFAULT_USER_ID, conversationId, chatId, Message.buildUserQuestionText(message), metaData);
    }

    public Flux<String> chat(String userId, String conversationId, String chatId, String message, Map<String, String> metaData) {
        return chat(userId, conversationId, chatId, Message.buildUserQuestionText(message), metaData);
    }

    public Flux<String> chat(String chatId, Message message) {
        return chat(DEFAULT_USER_ID, DEFAULT_CONVERSATION_ID, chatId, message);
    }

    public Flux<String> chat(String conversationId, String chatId, Message message) {
        return chat(DEFAULT_USER_ID, conversationId, chatId, message);
    }

    public Flux<String> chat(String conversationId, String chatId, List<Message> messages, Map<String, String> metaData) {
        return chat(DEFAULT_USER_ID, conversationId, chatId, messages, metaData);
    }

    public Flux<String> chat(String userId, String conversationId, String chatId, Message message) {
        return chat(userId, conversationId, chatId, Collections.singletonList(message), null);
    }

    public Flux<String> chat(String userId, String conversationId, String chatId, Message message, Map<String, String> metaData) {
        return chat(userId, conversationId, chatId, Collections.singletonList(message), metaData);
    }


    public Flux<String> chat(String userId, String conversationId, String chatId, List<Message> messages, Map<String, String> metaData) {
        Map<String, String> defaultMetaData = new HashMap<>();
        defaultMetaData.put(LOCAL_USER_ID, userId);
        defaultMetaData.put(LOCAL_CHAT_ID, chatId);
        defaultMetaData.put(LOCAL_CONVERSATION_ID, conversationId);
        if (metaData != null) {
            defaultMetaData.putAll(metaData);
        }
        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(properties.getBotId())
                        .userID(userId)
                        .conversationID(conversation(conversationId))
                        .readTimeout(properties.getReadTimeout())
                        .connectTimeout(properties.getConnectTimeout())
                        .messages(messages)
                        .metaData(defaultMetaData)
                        .autoSaveHistory(properties.getAutoSaveHistory())
                        .build();


        Flowable<ChatEvent> resp = coze.chat().stream(req);

        return Flux.from(resp).flatMap(this::process).handle(this::content);
    }
    private String conversation(String localConversationId) {
        String conversationId = null;
        if (StringUtils.isNotBlank(localConversationId)) {
            conversationId = dataMemory.get(getConversationIdKey(localConversationId));
        }
        if (StringUtils.isBlank(conversationId)) {
            conversationId = createConversation(localConversationId);
        }
        return conversationId;
    }

    public void cancelChat(String chatId) {
        cancelChat(DEFAULT_CONVERSATION_ID, chatId);
    }

    public String transcriptions(MultipartFile file) throws IOException {
        CreateTranscriptionsReq req = CreateTranscriptionsReq.builder().fileName(file.getOriginalFilename()).fileBytes(file.getBytes()).build();
        CreateTranscriptionsResp resp = coze.audio().transcription().create(req);
        return resp.getText();
    }

    public void cancelChat(String conversationId, String chatId) {
        doCancel(dataMemory.get(getConversationIdKey(conversationId)), dataMemory.get(getChatIdKey(conversationId, chatId)));
    }



    private String createConversation(String localConversationId) {
        CreateConversationReq build = CreateConversationReq.builder().botID(properties.getBotId()).build();
        String conversationId = coze.conversations().create(build).getConversation().getId();
        dataMemory.add(getConversationIdKey(localConversationId), conversationId);
        return conversationId;
    }

    public void doCancel(String conversationId, String chatId) {
        coze.chat().cancel(CancelChatReq.of(conversationId, chatId));
    }


    private String getConversationIdKey(String conversationId) {
        if (StringUtils.isBlank(conversationId)) {
            throw new RuntimeException("conversationId is null");
        }
        return CONVERSATION_PREFIX + conversationId;
    }

    private String getChatIdKey(String conversationId, String chatId) {
        if (StringUtils.isBlank(conversationId) || StringUtils.isBlank(chatId)) {
            throw new RuntimeException("conversationId or chatId is null");
        }
        return CHAT_PREFIX + conversationId + ":" + chatId;
    }

    public void clearConversation(String localConversationId, String conversationId) {
        dataMemory.clear(getConversationIdKey(localConversationId));
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
            sink.next(event.getChat().getLastError().getMsg());
            log.error("聊天失败message：{}, lastError: {}", event.getMessage(), event.getChat().getLastError());
        } else if (CONVERSATION_CHAT_CREATED.equals(event.getEvent())) {
            Chat chat = event.getChat();
            assert chat != null;
            Map<String, String> metaData = chat.getMetaData();
            dataMemory.add(getChatIdKey(metaData.get(LOCAL_CONVERSATION_ID), metaData.get(LOCAL_CHAT_ID)), chat.getID());
        }
    }

    /**
     * 执行端插件并返回结果
     * @param pluginEvent event
     * @return
     */
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
