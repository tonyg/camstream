package net.lshift.camcapture;

import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;

public class ArgumentDialog extends JDialog {
    public String[] optionNames;
    public JTextField[] optionFields;
    public boolean accepted = false;

    public ArgumentDialog(String[] optionNames, String[] optionValues) {
        super();

        JPanel content = new JPanel();
        content.setLayout(new GridLayout(optionNames.length + 1, 2));
        getContentPane().add(content);

        this.optionNames = optionNames;
        this.optionFields = new JTextField[optionNames.length];

        for (int i = 0; i < optionNames.length; i++) {
            content.add(new JLabel(optionNames[i]));
            optionFields[i] = new JTextField(optionValues[i]);
            content.add(optionFields[i]);
        }

        final ArgumentDialog outerThis = this;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    outerThis.hide();
                }
            });
        content.add(cancelButton);

        JButton acceptButton = new JButton("Accept");
        acceptButton.addActionListener(new ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
		    accepted = true;
                    outerThis.hide();
                }
            });
        content.add(acceptButton);

        pack();
        setModal(true);
    }

    public String[] getOptionValues() {
        if (accepted) {
            String[] result = new String[optionFields.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = optionFields[i].getText();
            }
            return result;
        } else {
            return null;
        }
    }
}
