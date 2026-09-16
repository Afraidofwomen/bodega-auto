package com.bodega.wms;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.state.AppStore;
import com.bodega.wms.ui.MainShell;
import com.bodega.wms.ui.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class WmsApp extends Application {
    private AppStore store;

    @Override
    public void start(Stage stage) {
        store = new AppStore();
        MainShell root = new MainShell(store);
        Scene scene = new Scene(root, 1280, 800);
        ThemeManager.apply(scene, store.darkThemeProperty().get());
        store.darkThemeProperty().addListener((obs, o, dark) -> ThemeManager.apply(scene, dark));

        stage.setTitle("WMS · Bodega automática");
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();

        try {
            store.connect();
        } catch (GatewayException ignored) {
        }
    }

    @Override
    public void stop() {
        if (store != null) {
            store.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
