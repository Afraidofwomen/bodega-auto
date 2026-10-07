package com.bodega.wms.swing;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

final class Ui {
    static final Color BG = Color.decode("#121212");
    static final Color SURFACE = Color.decode("#1E1E1E");
    static final Color TEXT = Color.decode("#E0E0E0");
    static final Color MUTED = Color.decode("#9AA0A6");
    static final Color LINE = Color.decode("#2C2C2C");
    static final Color PRIMARY = Color.decode("#0052CC");
    static final Color OK = Color.decode("#22C55E");
    static final Color WARN = Color.decode("#EAB308");
    static final Color ERR = Color.decode("#EF4444");
    static final Color PROCESS = Color.decode("#3B82F6");

    static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    static final Font CARD = new Font("SansSerif", Font.PLAIN, 16);
    static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);
    static final Font MICRO = new Font("SansSerif", Font.BOLD, 12);
    static final Font MONO = new Font("Monospaced", Font.BOLD, 16);
    static final Font HUGE = new Font("Monospaced", Font.BOLD, 36);

    private Ui() {
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(12, 14, 12, 14)
        ));
        return panel;
    }

    static JButton giant(String text, Color fill) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setBackground(fill);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 52));
        button.setMinimumSize(new Dimension(0, 52));
        return button;
    }

    static JTextField field(String prompt) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(MUTED);
                    g2.setFont(getFont());
                    Insets in = getInsets();
                    int y = (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
                    g2.drawString(prompt, in.left, y);
                    g2.dispose();
                }
            }
        };
        field.setFont(BODY);
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(PRIMARY);
        field.setSelectedTextColor(Color.WHITE);
        field.setOpaque(true);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE),
                new EmptyBorder(12, 12, 12, 12)
        ));
        return field;
    }

    static void stack(JComponent component, int width, int height) {
        component.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        if (component instanceof JLabel label) {
            label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        }
        component.setMinimumSize(new Dimension(width, height));
        component.setPreferredSize(new Dimension(width, height));
        component.setMaximumSize(new Dimension(width, height));
    }

    static void paint(JComponent component) {
        component.setBackground(BG);
        component.setForeground(TEXT);
        if (component instanceof JPanel panel) {
            panel.setOpaque(true);
        }
    }
}
