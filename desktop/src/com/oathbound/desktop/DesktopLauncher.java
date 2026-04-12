package com.oathbound.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.oathbound.core.OathboundGame;

public class DesktopLauncher {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 736);
        config.setForegroundFPS(60);
        config.setTitle("Oathbound: The Ten Trials");
        config.setResizable(false);
        
        // Starts the OpenGL context and hands control to our Game adapter
        new Lwjgl3Application(new OathboundGame(), config);
    }
}