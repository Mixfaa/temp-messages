package com.mixfa.tempmessages.route;

import com.mixfa.tempmessages.misc.Utils;
import com.mixfa.tempmessages.service.ReactiveChannelsService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;


@Slf4j
@Route("enter")
public class EnterRoute extends AppLayout {
    private final ReactiveChannelsService channelsService;

    public EnterRoute(ReactiveChannelsService channelsService) {
        this.channelsService = channelsService;
        setContent(Utils.wrapContent(makeContent()));
    }

    private Component makeContent() {
        var layout = new VerticalLayout();
        layout.setWidth("400px");

        layout.add(new H2("Enter channel"));

        var channelField = new TextField("Channel name");
        var passwordField = new TextField("Channel password");
        var loginButton = new Button("Login", _ -> {
            var name = channelField.getValue();
            var password = passwordField.getValue();

            var canJoin = channelsService.channelCheckCredentials(name, password);
            if (canJoin) {
                Notification.show("Login successful");

                UI.getCurrent().navigate(ChannelRoute.class, new RouteParameters(Map.of("name", name, "password", password)));
            } else {
                Notification.show("Can`t join channel");
            }
        });
        var createButton = new Button("Create", _ -> {
            var name = channelField.getValue();
            var password = passwordField.getValue();

            var exists = channelsService.channelExists(name);
            if (exists) {
                Notification.show("Channel already exists");
            } else {
                try {
                    channelsService.createChannel(name, password).block();

                    UI.getCurrent().navigate(ChannelRoute.class, new RouteParameters(Map.of("name", name, "password", password)));
                    Notification.show("Channel created");
                } catch (Exception e) {
                    Notification.show("Internal error");
                    log.error(e.getLocalizedMessage());
                }
            }
        });
        layout.add(new FormLayout(channelField, passwordField, new HorizontalLayout(loginButton, createButton)));

        return layout;
    }
}
