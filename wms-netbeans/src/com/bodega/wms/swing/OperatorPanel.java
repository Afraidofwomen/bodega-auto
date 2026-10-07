package com.bodega.wms.swing;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SlotId;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

final class OperatorPanel extends JPanel {
    private static final int COL = 520;

    private final SwingStore store;
    private final JLabel location = Ui.label("—", Ui.HUGE, Ui.TEXT);
    private final JLabel human = Ui.label("Selecciona un slot en el mapa", Ui.CARD, Ui.MUTED);
    private final JLabel skuView = Ui.label("SKU  —", Ui.MONO, Ui.PROCESS);
    private final JLabel qty = Ui.label("1 BIN", Ui.MICRO, Ui.MUTED);
    private final JTextField skuField = Ui.field("SKU del bin (store)");
    private final JLabel hint = Ui.label("Retrieve exige doble confirmación", Ui.MICRO, Ui.MUTED);
    private final JLabel feedback = Ui.label(" ", Ui.BODY, Ui.TEXT);
    private final JButton ioBtn = ghost("Simular bin en I/O");
    private boolean retrieveArmed;

    OperatorPanel(SwingStore store) {
        this.store = store;
        Ui.paint(this);
        setLayout(new BorderLayout());

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(new Dimension(COL, 640));
        col.setMinimumSize(new Dimension(COL, 520));

        col.add(heading("Módulo operario", Ui.TITLE, Ui.TEXT, 28));
        col.add(heading("Botones grandes · doble confirmación en retrieve", Ui.MICRO, Ui.MUTED, 20));
        col.add(Box.createVerticalStrut(12));

        JPanel pick = Ui.card();
        pick.setLayout(new BoxLayout(pick, BoxLayout.Y_AXIS));
        center(location);
        center(human);
        center(skuView);
        center(qty);
        location.setAlignmentX(CENTER_ALIGNMENT);
        human.setAlignmentX(CENTER_ALIGNMENT);
        skuView.setAlignmentX(CENTER_ALIGNMENT);
        qty.setAlignmentX(CENTER_ALIGNMENT);
        pick.add(location);
        pick.add(Box.createVerticalStrut(4));
        pick.add(human);
        pick.add(Box.createVerticalStrut(6));
        pick.add(skuView);
        pick.add(qty);
        Ui.stack(pick, COL, 168);
        col.add(pick);
        col.add(Box.createVerticalStrut(12));

        Ui.stack(skuField, COL, 48);
        col.add(skuField);
        col.add(Box.createVerticalStrut(12));

        JButton storeBtn = Ui.giant("STORE  ·  I/O → slot", Ui.PRIMARY);
        JButton retrieveBtn = Ui.giant("RETRIEVE  ·  slot → I/O", Ui.PROCESS);
        Ui.stack(storeBtn, COL, 52);
        Ui.stack(retrieveBtn, COL, 52);
        storeBtn.addActionListener(e -> doStore());
        retrieveBtn.addActionListener(e -> doRetrieve());
        col.add(storeBtn);
        col.add(Box.createVerticalStrut(8));
        col.add(retrieveBtn);
        col.add(Box.createVerticalStrut(6));
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        Ui.stack(hint, COL, 22);
        col.add(hint);

        JPanel safety = new JPanel(new GridLayout(1, 3, 8, 0));
        safety.setOpaque(false);
        JButton abort = Ui.giant("ABORT", Ui.ERR);
        JButton home = ghost("HOME");
        JButton reset = ghost("RESET (fault)");
        abort.addActionListener(e -> run("abort", () -> store.abort("abort")));
        home.addActionListener(e -> run("home", () -> store.abort("home")));
        reset.addActionListener(e -> run("reset", () -> store.abort("reset")));
        safety.add(abort);
        safety.add(home);
        safety.add(reset);
        Ui.stack(safety, COL, 52);
        col.add(Box.createVerticalStrut(10));
        col.add(safety);

        Ui.stack(ioBtn, COL, 48);
        ioBtn.addActionListener(e -> {
            store.placeDemoBinAtIo();
            feedback.setText("Bin de demo colocado en I/O");
        });
        col.add(Box.createVerticalStrut(8));
        col.add(ioBtn);
        Ui.stack(feedback, COL, 24);
        col.add(Box.createVerticalStrut(4));
        col.add(feedback);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 20));
        wrap.setOpaque(false);
        wrap.add(col);
        add(wrap, BorderLayout.CENTER);

        store.addListener(e -> refresh());
        refresh();
    }

    private void refresh() {
        ioBtn.setVisible(store.simulated());
        String raw = store.selectedSlot();
        retrieveArmed = false;
        hint.setText("Retrieve exige doble confirmación");
        if (!SlotId.isValid(raw)) {
            location.setText("—");
            human.setText("Selecciona un slot en el mapa");
            skuView.setText("SKU  —");
            qty.setText("1 BIN");
            return;
        }
        SlotId id = SlotId.parse(raw);
        Slot slot = store.inventory().get(raw);
        location.setText(id.aisleLabel());
        human.setText(id.human());
        skuView.setText("SKU  " + slot.displaySku());
        qty.setText(slot.occupied() ? "1 BIN  ·  ocupado" : "1 BIN  ·  destino libre");
    }

    private void doStore() {
        String slot = store.selectedSlot();
        if (!SlotId.isValid(slot)) {
            feedback.setText("Elige un slot destino");
            return;
        }
        run("store", () -> store.store(slot, skuField.getText()));
    }

    private void doRetrieve() {
        String slot = store.selectedSlot();
        if (!SlotId.isValid(slot)) {
            feedback.setText("Elige el slot a extraer");
            return;
        }
        if (!retrieveArmed) {
            retrieveArmed = true;
            hint.setText("Vuelve a pulsar RETRIEVE para confirmar " + slot);
            return;
        }
        retrieveArmed = false;
        hint.setText("Retrieve exige doble confirmación");
        run("retrieve", () -> store.retrieve(slot));
    }

    private void run(String name, Command command) {
        try {
            String cmdId = command.execute();
            feedback.setText(name + " enviado · " + cmdId.substring(0, Math.min(8, cmdId.length())));
        } catch (GatewayException e) {
            feedback.setText(e.getMessage());
        }
    }

    private static JLabel heading(String text, java.awt.Font font, java.awt.Color color, int height) {
        JLabel label = Ui.label(text, font, color);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        Ui.stack(label, COL, height);
        return label;
    }

    private static void center(JLabel label) {
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setAlignmentX(CENTER_ALIGNMENT);
    }

    private static JButton ghost(String text) {
        JButton button = Ui.giant(text, Ui.SURFACE);
        button.setForeground(Ui.TEXT);
        button.setBorder(BorderFactory.createLineBorder(Ui.LINE));
        button.setBorderPainted(true);
        return button;
    }

    @FunctionalInterface
    private interface Command {
        String execute() throws GatewayException;
    }
}
