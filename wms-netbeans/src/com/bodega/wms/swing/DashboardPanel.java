package com.bodega.wms.swing;

import com.bodega.wms.model.AlertItem;
import com.bodega.wms.model.Inventory;
import com.bodega.wms.model.SrmStatus;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

final class DashboardPanel extends JPanel {
    private final JLabel occupancy = kpiValue();
    private final JLabel occupancyHint = hint();
    private final JLabel mission = kpiValue();
    private final JLabel missionHint = hint();
    private final JLabel dock = kpiValue();
    private final JLabel dockHint = hint();
    private final JLabel srm = kpiValue();
    private final JLabel srmHint = hint();
    private final JLabel telemetry = Ui.label("X 0  Y 0  Z 0 mm", Ui.MONO, Ui.TEXT);
    private final DefaultListModel<String> alerts = new DefaultListModel<>();

    DashboardPanel(SwingStore store) {
        Ui.paint(this);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(Ui.label("Dashboard de control", Ui.TITLE, Ui.TEXT));
        head.add(Ui.label("Elevador 5 slots · KPIs en tiempo real", Ui.MICRO, Ui.MUTED));

        JPanel kpis = new JPanel(new GridLayout(1, 4, 10, 10));
        kpis.setOpaque(false);
        kpis.add(kpi("Ocupación", occupancy, occupancyHint));
        kpis.add(kpi("Misión", mission, missionHint));
        kpis.add(kpi("Andén I/O", dock, dockHint));
        kpis.add(kpi("Estado SRM", srm, srmHint));

        JList<String> list = new JList<>(alerts);
        list.setBackground(Ui.SURFACE);
        list.setForeground(Ui.TEXT);
        list.setFont(Ui.MICRO);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(Ui.LINE));
        scroll.setPreferredSize(new Dimension(0, 220));

        JPanel tel = Ui.card();
        tel.setLayout(new BoxLayout(tel, BoxLayout.Y_AXIS));
        tel.add(Ui.label("Telemetría", Ui.CARD, Ui.MUTED));
        tel.add(Box.createVerticalStrut(6));
        tel.add(telemetry);

        JPanel mid = new JPanel(new BorderLayout(0, 10));
        mid.setOpaque(false);
        mid.add(kpis, BorderLayout.NORTH);
        mid.add(scroll, BorderLayout.CENTER);
        mid.add(tel, BorderLayout.SOUTH);

        add(head, BorderLayout.NORTH);
        add(mid, BorderLayout.CENTER);

        store.addListener(e -> refresh(store));
        refresh(store);
    }

    private void refresh(SwingStore store) {
        Inventory inv = store.inventory();
        SrmStatus st = store.status();
        int used = inv.occupiedCount();
        int pct = (int) Math.round(inv.occupancyRatio() * 100);
        occupancy.setText(pct + "%");
        occupancyHint.setText(used + " / 5 slots");
        occupancy.setForeground(pct >= 90 ? Ui.ERR : pct >= 70 ? Ui.WARN : Ui.OK);

        mission.setText(st.state().busy() ? "1" : "0");
        missionHint.setText(st.mission() == null ? "sin misión" : st.mission());
        dock.setText(st.ioOccupied() ? "OCUPADO" : "LIBRE");
        dock.setForeground(st.ioOccupied() ? Ui.WARN : Ui.OK);
        dockHint.setText(st.forkOccupied() ? "horquilla con bin" : "horquilla vacía");
        srm.setText(st.estop() ? "ESTOP" : st.state().wire().toUpperCase());
        srm.setForeground(st.estop() || st.state().wire().equals("fault") ? Ui.ERR : Ui.PROCESS);
        srmHint.setText(st.homed() ? "homed" : "sin home");
        telemetry.setText(st.position().compact() + " mm   I/O " + (st.ioOccupied() ? "BIN" : "vacío"));

        alerts.clear();
        for (AlertItem item : store.alerts()) {
            alerts.addElement(item.clock() + "  " + item.title() + "  " + item.detail());
        }
    }

    private static JPanel kpi(String title, JLabel value, JLabel hint) {
        JPanel card = Ui.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(Ui.label(title, Ui.CARD, Ui.MUTED));
        card.add(value);
        card.add(hint);
        return card;
    }

    private static JLabel kpiValue() {
        JLabel label = Ui.label("—", new Font("Monospaced", Font.BOLD, 26), Ui.TEXT);
        return label;
    }

    private static JLabel hint() {
        return Ui.label("", Ui.MICRO, Ui.MUTED);
    }
}
