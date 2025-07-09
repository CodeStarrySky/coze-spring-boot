package com.wuch.flight.controller;

import com.wuch.coze.api.ChatClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;


@RequestMapping("/api/assistant")
@RestController
@RequiredArgsConstructor
public class AssistantController {

	private final ChatClient agent;


	@RequestMapping(path="/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(String chatId, String userMessage) {
		return agent.chat(chatId, userMessage);
	}

	@PostMapping("/cancel")
	public boolean cancel(String chatId) {
		agent.cancelChat(chatId);
		return true;
	}


	@PostMapping("/transcriptions")
	public String transcriptions(@RequestParam("audio") MultipartFile file) throws IOException {
		return agent.transcriptions(file);
	}
}
