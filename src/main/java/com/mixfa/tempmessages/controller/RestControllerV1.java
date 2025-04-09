package com.mixfa.tempmessages.controller;

import com.mixfa.tempmessages.model.*;
import com.mixfa.tempmessages.service.FileStorageService;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class RestControllerV1 {
    private final ReactiveChannelsService channelsService;
    private final FileStorageService fileStorageService;

    @GetMapping("/c/{channel}/{password}/messages")
    public Flux<Message.Dto> listMessages(@PathVariable String channel, @PathVariable String password) throws Exception {
        return channelsService.listMessages(channel, password, 0, 15).map(Message::toDto);
    }

    @PostMapping("/c/{channel}/{password}/create")
    public Mono<Channel> createChannel(@PathVariable String channel, @PathVariable String password) throws Exception {
        return channelsService.createChannel(channel, password);
    }

    @PostMapping("/c/{channel}/{password}/send-text/{text}")
    public Mono<Message.Dto> sendMessage(@PathVariable String channel, @PathVariable String password, @PathVariable String text) throws Exception {
        return channelsService.sendMessage(channel, password, new TextMessage(text)).map(Message::toDto);
    }

    @PostMapping("/c/{channel}/{password}/send-file")
    public Mono<Message.Dto> sendFile(@PathVariable String channel, @PathVariable String password, @RequestParam() MultipartFile file) throws Exception {
        var savedFile = fileStorageService.write(file);
        var fileMessage = new FileMessage(savedFile.path(), savedFile.id());
        return channelsService.sendMessage(channel, password, fileMessage).map(Message::toDto);
    }

    @GetMapping("/c/{channel}/{password}/get-file/{fileId}")
    public Mono<StreamingResponseBody> downloadFile(
            @PathVariable String channel, @PathVariable String password, @PathVariable String fileId
    ) throws Exception {
        return channelsService.getFile(channel, password, fileId).map(FileResponse::streamingResponse);
    }
}
