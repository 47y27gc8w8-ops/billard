package com.example.billard;

import javafx.application.Application;

/** Plain entry point for launching from a classpath jar, where an Application subclass cannot be main. */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(BilliardApplication.class, args);
    }
}
