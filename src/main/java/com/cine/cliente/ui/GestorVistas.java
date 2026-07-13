package com.cine.cliente.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class GestorVistas {
    private static Scene mainScene;

    public static void setMainScene(Scene scene) {
        mainScene = scene;
    }

    public static void navegarA(Parent vista) {
        if (mainScene != null) {
            mainScene.setRoot(vista);
        }
    }
}
