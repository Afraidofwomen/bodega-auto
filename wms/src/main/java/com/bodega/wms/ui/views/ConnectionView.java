package com.bodega.wms.ui.views;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.gateway.SrmNative;
import com.bodega.wms.model.GatewayKind;
import com.bodega.wms.state.AppStore;
import com.bodega.wms.state.SessionSettings;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class ConnectionView extends VBox {
    public ConnectionView(AppStore store) {
        getStyleClass().add("page");
        setSpacing(14);
        setPadding(new Insets(8, 4, 4, 4));
        setMaxWidth(640);

        Label title = new Label("Conexión SRM");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("MQTT al ESP32-S3 · o puente JNI al C++ de movimiento");
        subtitle.getStyleClass().add("page-subtitle");

        SessionSettings settings = store.settings();
        ComboBox<GatewayKind> kind = new ComboBox<>();
        kind.getItems().addAll(GatewayKind.values());
        kind.getSelectionModel().select(settings.getKind());
        kind.setMaxWidth(Double.MAX_VALUE);

        TextField host = field(settings.getBrokerHost());
        TextField port = field(String.valueOf(settings.getBrokerPort()));
        TextField device = field(settings.getDeviceId());
        TextField client = field(settings.getClientId());
        TextField user = field(settings.getUsername());
        PasswordField pass = new PasswordField();
        pass.setText(settings.getPassword());
        pass.getStyleClass().add("giant-field");

        Label nativeHint = new Label(SrmNative.available()
                ? "libsrm_bridge encontrada. El C++ puede recibir store/retrieve/abort."
                : "libsrm_bridge no cargada. Compila wms/native e instálala para el modo C++.");
        nativeHint.getStyleClass().add("micro");
        nativeHint.setWrapText(true);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        int r = 0;
        form.add(label("Pasarela"), 0, r);
        form.add(kind, 1, r++);
        form.add(label("Broker MQTT"), 0, r);
        form.add(host, 1, r++);
        form.add(label("Puerto"), 0, r);
        form.add(port, 1, r++);
        form.add(label("device_id"), 0, r);
        form.add(device, 1, r++);
        form.add(label("client_id"), 0, r);
        form.add(client, 1, r++);
        form.add(label("Usuario"), 0, r);
        form.add(user, 1, r++);
        form.add(label("Clave"), 0, r);
        form.add(pass, 1, r);
        form.getStyleClass().add("card");
        form.setPadding(new Insets(16));

        Label feedback = new Label("Arranca en simulador para operar sin hardware.");
        feedback.getStyleClass().add("feedback");

        Button connect = new Button("Conectar");
        connect.getStyleClass().addAll("giant-btn", "btn-primary");
        connect.setPrefHeight(52);
        Button disconnect = new Button("Desconectar");
        disconnect.getStyleClass().addAll("giant-btn", "btn-ghost");
        disconnect.setPrefHeight(52);
        connect.setMaxWidth(Double.MAX_VALUE);
        disconnect.setMaxWidth(Double.MAX_VALUE);
        HBox actions = new HBox(10, connect, disconnect);
        actions.getChildren().forEach(n -> javafx.scene.layout.HBox.setHgrow(n, javafx.scene.layout.Priority.ALWAYS));

        connect.setOnAction(e -> {
            settings.setKind(kind.getValue());
            settings.setBrokerHost(host.getText().trim());
            try {
                settings.setBrokerPort(Integer.parseInt(port.getText().trim()));
            } catch (NumberFormatException ex) {
                feedback.setText("Puerto inválido");
                return;
            }
            settings.setDeviceId(device.getText().trim().isEmpty() ? "srm1" : device.getText().trim());
            settings.setClientId(client.getText().trim().isEmpty() ? "wms-ui" : client.getText().trim());
            settings.setUsername(user.getText());
            settings.setPassword(pass.getText());
            try {
                store.connect();
                feedback.setText("Conectado · " + settings.getKind().label());
            } catch (GatewayException ex) {
                feedback.setText(ex.getMessage());
                store.flash(false);
            }
        });
        disconnect.setOnAction(e -> {
            store.disconnect();
            feedback.setText("Desconectado");
        });

        Label topics = new Label(
                "Tópicos (device_id=" + settings.getDeviceId() + "):\n"
                        + "  asrs/{id}/cmd/store|retrieve|abort\n"
                        + "  asrs/{id}/status   asrs/{id}/event   asrs/{id}/inventory"
        );
        topics.getStyleClass().add("mono-block");
        topics.setWrapText(true);

        getChildren().addAll(title, subtitle, form, nativeHint, actions, feedback, topics);
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private static TextField field(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("giant-field");
        return field;
    }
}
