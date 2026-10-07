package com.bodega.wms.swing;

import com.bodega.wms.model.Slot;
import com.bodega.wms.model.SlotId;
import com.bodega.wms.model.SrmStatus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

final class MapPanel extends JPanel {
    private final SwingStore store;
    private final Map<String, JButton> cells = new LinkedHashMap<>();
    private final JButton io = new JButton("I/O");
    private final JLabel crane = Ui.label("SRM", Ui.MONO, Ui.PROCESS);
    private final JLabel selected = Ui.label("Selecciona un slot", Ui.BODY, Ui.TEXT);

    MapPanel(SwingStore store) {
        this.store = store;
        Ui.paint(this);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Ui.label("Columna elevador", Ui.TITLE, Ui.TEXT));
        head.add(Ui.label("Rojo = ocupado · Verde = vacío · I/O abajo", Ui.MICRO, Ui.MUTED));

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        for (int level = 5; level >= 1; level--) {
            String id = "S" + level;
            JButton cell = slotButton(id);
            cells.put(id, cell);
            column.add(cell);
            column.add(Box.createVerticalStrut(8));
        }
        styleSlot(io, false, false);
        io.setEnabled(false);
        column.add(io);

        JPanel shaft = Ui.card();
        shaft.setLayout(new BorderLayout());
        shaft.add(crane, BorderLayout.SOUTH);

        JPanel map = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        map.setOpaque(false);
        map.add(column);
        map.add(shaft);

        add(head, BorderLayout.NORTH);
        add(map, BorderLayout.CENTER);
        add(selected, BorderLayout.SOUTH);

        store.addListener(e -> refresh());
        refresh();
    }

    private JButton slotButton(String id) {
        JButton button = new JButton(id);
        button.setFocusPainted(false);
        button.addActionListener(e -> store.setSelectedSlot(id));
        return button;
    }

    private void refresh() {
        String sel = store.selectedSlot();
        for (var entry : cells.entrySet()) {
            Slot slot = store.inventory().get(entry.getKey());
            styleSlot(entry.getValue(), slot.occupied(), entry.getKey().equals(sel));
            entry.getValue().setText(slot.slotId() + "  " + (slot.occupied() ? slot.displaySku() : "libre"));
        }
        SrmStatus st = store.status();
        styleSlot(io, st.ioOccupied(), false);
        io.setText(st.ioOccupied() ? "I/O  BIN" : "I/O  vacío");
        crane.setText("SRM  " + st.state().wire() + "  " + st.position().compact());
        if (sel == null || !SlotId.isValid(sel)) {
            selected.setText("Selecciona un slot");
        } else {
            Slot slot = store.inventory().get(sel);
            selected.setText(sel + "  ·  " + (slot.occupied() ? slot.displaySku() : "libre"));
        }
    }

    private static void styleSlot(JButton button, boolean full, boolean selected) {
        Color fill = full ? new Color(90, 28, 28) : new Color(22, 64, 38);
        button.setOpaque(true);
        button.setBackground(fill);
        button.setForeground(Ui.TEXT);
        button.setFont(Ui.MONO);
        button.setPreferredSize(new Dimension(0, 56));
        button.setBorder(selected ? new LineBorder(Ui.PRIMARY, 2) : new LineBorder(Ui.LINE));
    }
}
