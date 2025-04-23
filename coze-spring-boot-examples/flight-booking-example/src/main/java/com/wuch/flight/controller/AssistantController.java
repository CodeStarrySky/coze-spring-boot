package com.wuch.flight.controller;

import com.wuch.coze.api.ChatClient;
import com.wuch.coze.api.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RequestMapping("/api/assistant")
@RestController
@RequiredArgsConstructor
public class AssistantController {

	private final ChatClient agent;


	@RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(String chatId, String userMessage) {
		return agent.chat(chatId, userMessage);
	}


}
