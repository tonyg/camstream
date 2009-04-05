package net.lshift.camcapture;

import javax.swing.JOptionPane;

public class SwingUtil {
    public static void complain(String title, String prefix, Exception e) {
        e.printStackTrace();
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        String message = (prefix == null) ? sw.toString() : prefix + "\n" + sw.toString();
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void complainFatal(String title, String prefix, Exception e) {
        complain(title, prefix, e);
        System.exit(1);
    }
}
