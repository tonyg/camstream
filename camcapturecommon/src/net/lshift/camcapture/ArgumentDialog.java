/*
 *  Camstream, an AMQP-based video streaming toolkit.
 *  Copyright (C) 2007-2009 LShift Ltd. <query@lshift.net>
 *  Copyright (C) 2010-2012 Tony Garnock-Jones <tonygarnockjones@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

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
