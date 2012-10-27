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

package net.lshift.camdisplay;

// Incorporating techniques and some code from
// LiveCam.java by Jochen Broz on 19.02.05,
// http://lists.apple.com/archives/quicktime-java/2005/Feb/msg00062.html

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownSignalException;
import net.lshift.camcapture.ArgumentDialog;
import net.lshift.camcapture.SwingUtil;

public class Main {
    public static void main(String args[]) {
	try {
	    if (args.length < 4) {
		System.err.println("Usage: camdisplay <hostname> <exchangename> <nickname> <mixerSpec>");
                ArgumentDialog d = new ArgumentDialog(new String[] {
                    "Host Name",
                    "Channel Name",
                    "Nickname",
                    "Audio Output Device"
                }, new String[] {
                    "dev.rabbitmq.com",
                    "lfish",
                    "CHANGEME",
                    "Audio"
                });
                d.show();
                args = d.getOptionValues();
                if (args == null) {
                    System.exit(0);
                }
	    }

	    String host = args[0];
	    String exch = args[1];
            String nickname = args[2];
            String mixerSpec = args[3];
	    new Main(host, exch, nickname, mixerSpec);
	} catch (Exception e) {
            SwingUtil.complainFatal("Error", null, e);
	}
    }

    public JFrame frame;
    public JPanel panel;
    public Map componentMap;
    public JScrollPane textScroller;
    public JTextArea textOutput;
    public JTextField textInput;

    public Connection conn;
    public Channel ch;

    public Main(String host, String exch, String nickname, final String mixerSpec)
	throws IOException
    {
	frame = new JFrame("RabbitCam: " + host + "/" + exch);
        panel = new JPanel();
        componentMap = new HashMap();

        setupWindowDressing(exch, nickname, frame, panel);
        frame.pack();
	frame.show();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textInput.requestFocusInWindow();

        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(host);
        cf.setRequestedHeartbeat(0);
        conn = cf.newConnection();

	ch = conn.createChannel();

	ch.exchangeDeclare(exch, "fanout");

	String queueName = ch.queueDeclare().getQueue();
	ch.queueBind(queueName, exch, "");
	ch.basicConsume(queueName, true,
			new DefaultConsumer(ch) {
                            public void handleShutdownSignal(String consumerTag, ShutdownSignalException s) {
                                if (s.getReason() instanceof java.io.EOFException) {
                                    JOptionPane.showMessageDialog(frame,
                                                                  "AMQP server disconnected.",
                                                                  "Connection closed",
                                                                  JOptionPane.ERROR_MESSAGE);
                                } else {
                                    SwingUtil.complain("Connection closed", null, s);
                                }
                                System.exit(1);
                            }

			    public void handleDelivery(String consumerTag,
                                                       Envelope envelope,
						       AMQP.BasicProperties properties,
						       byte[] body)
				throws IOException
			    {
                                String routingKey = envelope.getRoutingKey();
                                String contentType = properties.getContentType();

                                if (contentType.equals("text/plain")) {
                                    handleText(routingKey, new String(body));
                                    return;
                                }

                                CamstreamComponent comp;
                                if (!componentMap.containsKey(routingKey)) {
                                    comp = new CamstreamComponent(mixerSpec);
                                    addComponent(routingKey, comp);
                                    frame.pack();
                                } else {
                                    comp = (CamstreamComponent) componentMap.get(routingKey);
                                }
                                if (comp.handleDelivery(contentType, body)) {
                                    frame.pack();
                                }
                            }
			});
    }

    public void handleText(String nickname, String message) {
        textOutput.append(nickname + ": " + message + "\n");
        textOutput.setCaretPosition(textOutput.getText().length() - 1);
    }

    public void setupWindowDressing(final String exchangeName,
                                    final String nickname,
                                    JFrame frame,
                                    JPanel componentPanel)
    {
        componentPanel.setBackground(Color.WHITE);
        componentPanel.setBorder(new TitledBorder("Channel '" + exchangeName + "'"));

        ImageIcon icon = new ImageIcon(Main.class.getResource("/resources/RabbitMQLogo.png"));
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(icon.getIconWidth() + 40,
                                                 icon.getIconHeight() + 40));

        Box textInputBox = new Box(BoxLayout.X_AXIS);
        textInputBox.add(new JLabel(nickname));
        textInputBox.add((textInput = new JTextField()));

        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.add((textScroller =
                       new JScrollPane((textOutput = new JTextArea(7, 0)),
                                       ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                       ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)),
                      BorderLayout.CENTER);
        textPanel.add(textInputBox, BorderLayout.SOUTH);

        textOutput.setEditable(false);
        textInput.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String message = textInput.getText();
                    textInput.setText("");
                    try {
                        ch.basicPublish(exchangeName, nickname,
                                        MessageProperties.TEXT_PLAIN,
                                        message.getBytes());
                    } catch (IOException ioe) {
                        System.err.println("IOException sending text message");
                        ioe.printStackTrace();
                    }
                }
            });

        JPanel main = new JPanel();
        main.setBackground(Color.WHITE);
        main.setLayout(new BorderLayout());
        main.add(iconLabel, BorderLayout.NORTH);
        main.add(componentPanel, BorderLayout.CENTER);
        main.add(textPanel, BorderLayout.SOUTH);

        frame.getContentPane().add(main);

    }

    public void addComponent(String routingKey, CamstreamComponent comp) {
	JLabel statusLabel = new JLabel();
	comp.setStatusLabel(statusLabel);

	JPanel statusPanel = new JPanel();
	statusPanel.setLayout(new BorderLayout());
	statusPanel.add(new JLabel(routingKey), BorderLayout.WEST);
	statusPanel.add(statusLabel, BorderLayout.EAST);

        JPanel holder = new JPanel();
        holder.setLayout(new BorderLayout());
        holder.add(comp, BorderLayout.CENTER);
        holder.add(statusPanel, BorderLayout.SOUTH);

        this.panel.add(holder);
        componentMap.put(routingKey, comp);
    }
}
