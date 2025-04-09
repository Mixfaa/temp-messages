package com.mixfa.tempmessages.route;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("")
public class IndexRedirectRoute extends Div implements BeforeEnterObserver {

    public IndexRedirectRoute() {
        this.getUI().ifPresent(ui -> {
            ui.navigate(EnterRoute.class);
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.rerouteTo(EnterRoute.class);
    }
}
