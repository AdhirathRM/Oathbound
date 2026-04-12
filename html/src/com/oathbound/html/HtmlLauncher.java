package com.oathbound.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.oathbound.core.OathboundGame;

public class HtmlLauncher extends GwtApplication {

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(960, 552);
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new OathboundGame();
    }
}
