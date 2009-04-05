package net.lshift.camdisplay;

// Incorporating techniques and some code from
// LiveCam.java by Jochen Broz on 19.02.05,
// http://lists.apple.com/archives/quicktime-java/2005/Feb/msg00062.html

import java.util.Map;
import java.util.HashMap;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.border.TitledBorder;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownSignalException;

import net.lshift.camcapture.ArgumentDialog;
import net.lshift.camcapture.SwingUtil;

public class Main {
    public static void main(String args[]) {
	try {
	    if (args.length < 2) {
		System.err.println("Usage: camdisplay <hostname> <exchangename> <nickname>");
                ArgumentDialog d = new ArgumentDialog(new String[] {
                    "Host Name",
                    "Channel Name",
                    "Nickname"
                }, new String[] {
                    "dev.rabbitmq.com",
                    "lfish",
                    "CHANGEME"
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
	    new Main(host, exch, nickname);
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

    public Main(String host, String exch, String nickname)
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

        ConnectionParameters p = new ConnectionParameters();
        p.setUsername("camstream");
        p.setPassword("camstream");
        p.setVirtualHost("/camstream");
        p.setRequestedHeartbeat(0);
        conn = new ConnectionFactory(p).newConnection(host);

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
                                String contentType = properties.contentType;

                                if (contentType.equals("text/plain")) {
                                    handleText(routingKey, new String(body));
                                    return;
                                }

                                CamstreamComponent comp;
                                if (!componentMap.containsKey(routingKey)) {
                                    comp = new CamstreamComponent();
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
