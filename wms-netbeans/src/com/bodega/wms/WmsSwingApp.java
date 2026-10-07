package com.bodega.wms;

import com.bodega.wms.swing.WmsFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class WmsSwingApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            WmsFrame frame = new WmsFrame();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
