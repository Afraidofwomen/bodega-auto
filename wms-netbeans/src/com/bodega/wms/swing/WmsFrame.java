package com.bodega.wms.swing;

import com.bodega.wms.gateway.GatewayException;
import com.bodega.wms.model.ConnectionState;
import com.bodega.wms.model.SrmStatus;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public final class WmsFrame extends JFrame {
    private final SwingStore store = new SwingStore();
    private final JLabel badge = new JLabel("OFF");
    private final JLabel device = Ui.label("srm1", Ui.MONO, Ui.TEXT);
    private final JLabel conn = Ui.label("Offline", Ui.MICRO, Ui.TEXT);

    public WmsFrame() {
        super("WMS · Bodega automática");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 700));
        setSize(1280, 800);
        getContentPane().setBackground(Ui.BG);
        setLayout(new BorderLayout());

        JPanel nav = new JPanel();
        nav.setBackground(Color.decode("#161616"));
        nav.setPreferredSize(new Dimension(196, 0));
        nav.setBorder(new EmptyBorder(20, 14, 20, 14));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        JLabel brand = Ui.label("WMS", Ui.TITLE, Ui.PRIMARY);
        JLabel sub = Ui.label("Bodega automática", Ui.MICRO, Ui.MUTED);
        nav.add(brand);
        nav.add(sub);
        nav.add(Box.createVerticalStrut(18));

        CardLayout cards = new CardLayout();
        JPanel pages = new JPanel(cards);
        pages.setBackground(Ui.BG);
        pages.add(new DashboardPanel(store), "dash");
        pages.add(new MapPanel(store), "map");
        pages.add(new OperatorPanel(store), "op");
        pages.add(new ConnectionPanel(store), "link");

        nav.add(navBtn("Dashboard", () -> cards.show(pages, "dash")));
        nav.add(Box.createVerticalStrut(8));
        nav.add(navBtn("Mapa", () -> cards.show(pages, "map")));
        nav.add(Box.createVerticalStrut(8));
        nav.add(navBtn("Operario", () -> cards.show(pages, "op")));
        nav.add(Box.createVerticalStrut(8));
        nav.add(navBtn("Conexión", () -> cards.show(pages, "link")));

        badge.setOpaque(true);
        badge.setFont(new Font("Monospaced", Font.BOLD, 12));
        badge.setForeground(Color.WHITE);
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        paintBadge("OFF", Ui.MUTED);

        JPanel top = new JPanel();
        top.setBackground(Ui.SURFACE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Ui.LINE),
                new EmptyBorder(10, 18, 10, 18)
        ));
        top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
        top.add(badge);
        top.add(Box.createHorizontalStrut(12));
        top.add(device);
        top.add(Box.createHorizontalGlue());
        top.add(conn);

        add(nav, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);
        add(pages, BorderLayout.CENTER);

        store.addListener(evt -> refreshBar());
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                store.disconnect();
            }
        });

        try {
            store.connect();
        } catch (GatewayException ignored) {
        }
        refreshBar();
    }

    public static void main(String[] args) {
        com.bodega.wms.WmsSwingApp.main(args);
    }

    private void refreshBar() {
        SrmStatus st = store.status();
        device.setText(st.deviceId());
        ConnectionState cs = store.connection();
        conn.setText(cs.label() + (store.connectionDetail().isBlank() ? "" : " · " + store.connectionDetail()));
        conn.setForeground(cs == ConnectionState.ONLINE ? Ui.OK : Ui.ERR);
        String wire = st.estop() ? "ESTOP" : st.state().wire().toUpperCase();
        Color tone = switch (st.state()) {
            case IDLE -> Ui.OK;
            case FAULT -> Ui.ERR;
            case UNKNOWN -> Ui.MUTED;
            default -> Ui.PROCESS;
        };
        if (st.estop()) {
            tone = Ui.ERR;
        }
        paintBadge(wire, tone);
    }

    private void paintBadge(String text, Color bg) {
        badge.setText(text);
        badge.setBackground(bg);
    }

    private static JButton navBtn(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(168, 48));
        button.setBackground(Ui.BG);
        button.setForeground(Ui.TEXT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.addActionListener(e -> action.run());
        return button;
    }
}
