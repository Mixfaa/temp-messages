package com.mixfa.tempmessages.controller;

import com.mixfa.tempmessages.misc.Utils;
import com.mixfa.tempmessages.model.FileMessage;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.model.TextMessage;
import com.mixfa.tempmessages.service.FileStorageService;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2")
@RequiredArgsConstructor
public class RestControllerV2 {
    private final ReactiveChannelsService channelsService;
    private final FileStorageService fileStorageService;

    @GetMapping("/c/{channel}/{password}/listen")
    public Flux<String> listenMessages(@PathVariable String channel, @PathVariable String password) throws Exception {
        return channelsService.listenMessages(channel, password)
                .map(it -> Utils.messageToPrettyString(it) + "\n")
                .onErrorReturn("Error");
    }

    @GetMapping("/c/{channel}/{password}/messages")
    public Mono<String> listMessages(@PathVariable String channel, @PathVariable String password) throws Exception {
        return channelsService.listMessages(channel, password, 0, 15)
                .collectList()
                .map(it -> it.reversed().stream().map(Utils::messageToPrettyString).collect(Collectors.joining("\n"))
                        + "\n")
                .onErrorReturn("Error");
    }

    @PostMapping("/c/{channel}/{password}/create")
    public Mono<String> createChannel(@PathVariable String channel, @PathVariable String password) throws Exception {
        return channelsService.createChannel(channel, password)
                .map(Utils::channelToPrettyString)
                .map(it -> it + "\n")
                .onErrorReturn("Error");
    }

    @PostMapping("/c/{channel}/{password}/send-text/{text}")
    public Mono<String> sendMessage(@PathVariable String channel, @PathVariable String password,
                                    @PathVariable String text) throws Exception {
        return channelsService.sendMessage(channel, password, new TextMessage(text))
                .map(Utils::messageToPrettyString)
                .map(it -> it + "\n")
                .onErrorReturn("Error");
    }

    @PostMapping("/c/{channel}/{password}/send-file")
    public Mono<String> sendFile(@PathVariable String channel, @PathVariable String password,
                                 @RequestParam() MultipartFile file) throws Exception {
        var savedFile = fileStorageService.write(file);
        var fileMessage = new FileMessage(savedFile.path(), savedFile.id());
        return channelsService.sendMessage(channel, password, fileMessage)
                .map(Utils::messageToPrettyString)
                .map(it -> it + "\n")
                .onErrorReturn("Error");
    }

    @GetMapping("/c/{channel}/{password}/get-file/{fileId}")
    public Mono<StreamingResponseBody> downloadFile(
            @PathVariable String channel, @PathVariable String password, @PathVariable String fileId) throws Exception {
        return channelsService.getFile(channel, password, fileId).map(FileResponse::streamingResponse);
    }
}
