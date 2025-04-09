package com.mixfa.tempmessages.route;

import com.mixfa.tempmessages.misc.Utils;
import com.mixfa.tempmessages.model.FileMessage;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.model.Message;
import com.mixfa.tempmessages.model.TextMessage;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Route("channel/:name/:password")
public class ChannelRoute extends AppLayout implements BeforeEnterObserver {
    private final ReactiveChannelsService channelsService;
    private String channelName;
    private String channelPassword;
    private Disposable chatSubscription;

    public ChannelRoute(ReactiveChannelsService channelsService) {
        this.channelsService = channelsService;
    }

    private ComponentRenderer<Component, Message> makeMessageRenderer() {
        return new ComponentRenderer<>(message -> {
            HorizontalLayout cardLayout = new HorizontalLayout();
            cardLayout.setMargin(true);

            VerticalLayout infoLayout = new VerticalLayout();
            infoLayout.setSpacing(false);
            infoLayout.setPadding(false);
            var avatar = new Avatar() {{
                setHeight("64px");
                setWidth("64px");
            }};
            switch (message) {
                case TextMessage(String text) -> infoLayout.add(new Div(new Text(text)));
                case FileMessage(String _, String id) -> {

                    FileResponse fileResponse;
                    try {
                        fileResponse = channelsService.getFile(channelName, channelPassword, id).block();
                    } catch (Exception e) {
                        log.error(e.getLocalizedMessage());
                        break;
                    }
                    if (fileResponse == null) {
                        Notification.show("Error, can`t find file");
                        log.error("fileResponse is null, id = {}", id);
                        break;
                    }

                    var anchor = prepareAnchor(fileResponse);
                    infoLayout.add(new Div(anchor));
                }
            }

            cardLayout.add(avatar, infoLayout);
            return cardLayout;
        });
    }

    @SneakyThrows
    private Component makeContent() {
        var layout = new VerticalLayout();
        layout.setWidth("1000px");
        layout.setHeightFull();

        var messageList = new VirtualList<Message>();
        messageList.setRenderer(makeMessageRenderer());

        var messagesImmutableList = channelsService.listMessages(channelName, channelPassword, 0, 20)
                .collectList().block();

        var messages = new CopyOnWriteArrayList<>(messagesImmutableList != null ? messagesImmutableList : List.of());

        messageList.setItems(messages);

        var ui = UI.getCurrent();
        chatSubscription = channelsService.listenMessages(channelName, channelPassword)
                .subscribe(newMessage -> {
                    messages.addLast(newMessage);
                    ui.access(() -> messageList.setItems(messages));
                });

        var messageInput = new MessageInput(event -> {
            try {
                channelsService.sendMessage(channelName, channelPassword, new TextMessage(event.getValue())).subscribe();
            } catch (Exception e) {
                Notification.show("Internal error");
            }
        });
        messageInput.setWidthFull();

        var buffer = new MemoryBuffer();
        var fileUpload = new Upload(buffer);
        fileUpload.addSucceededListener(_ -> {
            try {
                channelsService.sendFileMessage(channelName, channelPassword, buffer.getFileName(), buffer.getInputStream()).subscribe();
            } catch (Exception e) {
                Notification.show("Error, during sending file");
                log.error(e.getLocalizedMessage());
            }
        });

        layout.add(messageList, new VerticalLayout(messageInput, fileUpload) {{
            setWidthFull();
        }});
        return layout;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        chatSubscription.dispose();
        super.onDetach(detachEvent);
    }

    private static Anchor prepareAnchor(FileResponse fileResponse) {
        StreamResource resource = new StreamResource(fileResponse.filename(), () -> {
            try {
                PipedOutputStream pos = new PipedOutputStream();
                PipedInputStream pis = new PipedInputStream(pos);

                try {
                    fileResponse.streamingResponse().writeTo(pos);
                    pos.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Streaming failed", ex);
                }

                return pis;
            } catch (IOException e) {
                throw new RuntimeException("Pipe creation failed", e);
            }
        });

        return new Anchor(resource, fileResponse.filename()) {{
            getElement().setAttribute("download", true);
        }};
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var parameter = event.getRouteParameters();

        var name = parameter.get("name");
        var password = parameter.get("password");

        if (name.isEmpty() || password.isEmpty()) {
            Notification.show("Error, null credentials");
            UI.getCurrent().navigate(EnterRoute.class);
            return;
        }

        this.channelName = name.get();
        this.channelPassword = password.get();

        setContent(Utils.wrapContent(makeContent()));
    }
}
