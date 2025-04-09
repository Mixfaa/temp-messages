package com.mixfa.tempmessages.misc;

import com.mixfa.tempmessages.model.Channel;
import com.mixfa.tempmessages.model.FileMessage;
import com.mixfa.tempmessages.model.Message;
import com.mixfa.tempmessages.model.TextMessage;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import lombok.experimental.UtilityClass;

import java.text.MessageFormat;
import java.time.Duration;

@UtilityClass
public class Utils {
    public static Duration TIME_TO_LIVE = Duration.ofDays(1);

    public static Div wrapContent(Component component) {
        var div = new Div();
        div.setSizeFull();
        div.getStyle()
                .set("display", "flex")
                .set("justify-content", "center");
        div.add(component);
        return div;
    }

    public static String messageToPrettyString(Message message) {
        return switch (message) {
            case TextMessage(String text) -> text;
            case FileMessage(String _, String id) -> "File message: " + id;
        };
    }

    public static String channelToPrettyString(Channel channel) {
        return MessageFormat.format("Channel: {0}", channel.name());
    }
}
