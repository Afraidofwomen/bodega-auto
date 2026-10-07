package com.bodega.wms.swing;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.model.GatewayKind;
import com.bodega.wms.state.SessionSettings;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

final class ConnectionPanel extends JPanel {
    ConnectionPanel(SwingStore store) {
        Ui.paint(this);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 24, 16, 24));

        SessionSettings s = store.settings();
        JComboBox<GatewayKind> kind = new JComboBox<>(GatewayKind.values());
        kind.setSelectedItem(s.getKind());
        kind.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GatewayKind gk) {
                    setText(gk.label());
                }
                return this;
            }
        });
        JTextField host = field(s.getBrokerHost());
        JTextField port = field(String.valueOf(s.getBrokerPort()));
        JTextField device = field(s.getDeviceId());
        JTextField client = field(s.getClientId());
        JTextField user = field(s.getUsername());
        JPasswordField pass = new JPasswordField(s.getPassword());

        JPanel form = Ui.card();
        form.setLayout(new GridLayout(0, 2, 10, 8));
        form.add(Ui.label("Pasarela", Ui.MICRO, Ui.MUTED));
        form.add(kind);
        form.add(Ui.label("Broker MQTT", Ui.MICRO, Ui.MUTED));
        form.add(host);
        form.add(Ui.label("Puerto", Ui.MICRO, Ui.MUTED));
        form.add(port);
        form.add(Ui.label("device_id", Ui.MICRO, Ui.MUTED));
        form.add(device);
        form.add(Ui.label("client_id", Ui.MICRO, Ui.MUTED));
        form.add(client);
        form.add(Ui.label("Usuario", Ui.MICRO, Ui.MUTED));
        form.add(user);
        form.add(Ui.label("Clave", Ui.MICRO, Ui.MUTED));
        form.add(pass);

        JLabel feedback = Ui.label("Arranca en simulador para operar sin hardware.", Ui.BODY, Ui.TEXT);
        JButton connect = Ui.giant("Conectar", Ui.PRIMARY);
        JButton disconnect = Ui.giant("Desconectar", Ui.SURFACE);
        disconnect.setForeground(Ui.TEXT);
        connect.addActionListener(e -> {
            s.setKind((GatewayKind) kind.getSelectedItem());
            s.setBrokerHost(host.getText().trim());
            try {
                s.setBrokerPort(Integer.parseInt(port.getText().trim()));
            } catch (NumberFormatException ex) {
                feedback.setText("Puerto inválido");
                return;
            }
            s.setDeviceId(device.getText().trim().isEmpty() ? "srm1" : device.getText().trim());
            s.setClientId(client.getText().trim().isEmpty() ? "wms-ui" : client.getText().trim());
            s.setUsername(user.getText());
            s.setPassword(new String(pass.getPassword()));
            try {
                store.connect();
                feedback.setText("Conectado · " + s.getKind().label());
            } catch (GatewayException ex) {
                feedback.setText(ex.getMessage());
            }
        });
        disconnect.addActionListener(e -> {
            store.disconnect();
            feedback.setText("Desconectado");
        });

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        actions.setOpaque(false);
        actions.add(connect);
        actions.add(disconnect);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(Ui.label("Conexión SRM", Ui.TITLE, Ui.TEXT));
        col.add(Ui.label("MQTT al ESP32 · simulador · o JNI C++", Ui.MICRO, Ui.MUTED));
        col.add(Box.createVerticalStrut(12));
        form.setMaximumSize(new Dimension(640, 280));
        col.add(form);
        col.add(Box.createVerticalStrut(10));
        actions.setMaximumSize(new Dimension(640, 52));
        col.add(actions);
        col.add(Box.createVerticalStrut(8));
        col.add(feedback);
        col.add(Box.createVerticalStrut(8));
        col.add(Ui.label("Tópicos: asrs/{id}/cmd/store|retrieve|abort  ·  status  event  inventory", Ui.MICRO, Ui.MUTED));

        add(col, BorderLayout.NORTH);
    }

    private static JTextField field(String value) {
        JTextField field = new JTextField(value);
        field.setFont(Ui.BODY);
        return field;
    }
}
