package net.lshift.camcapture;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.lshift.camcapture.AMQVideoSender;

public abstract class CaptureBase {
    public CaptureBase(String programName, String args[])
        throws Exception
    {
        if (args.length < 3) {
            System.err.println("Usage: " + programName + " <hostname> <exchangename> <routingkey>");
            System.err.println("        [<framerate> [<width> [<height>]]]");
            ArgumentDialog d = new ArgumentDialog(new String[] {
                "Host Name",
                "Channel Name",
                "Nickname",
                "Frame rate",
                "Width",
                "Height"
            }, new String[] {
                "dev.rabbitmq.com",
                "lfish",
                "CHANGEME",
                "5",
                "320",
                "240"
            });
            d.show();
            args = d.getOptionValues();
            if (args == null) {
                System.exit(0);
            }
        }

        final String host = args[0];
        final String exch = args[1];
        String routingKey = args[2];
        int targetFrameRate = (args.length > 3 ? Integer.parseInt(args[3]) : 0);
        int desiredWidth = (args.length > 4 ? Integer.parseInt(args[4]) : 176);
        int desiredHeight = (args.length > 5 ? Integer.parseInt(args[5]) : 144);

        openControlWindow(host, exch, routingKey);

        String stageMessage = "during startup";
        try {
            stageMessage = "connecting to broker";
            AMQVideoSender sender = new AMQVideoSender(host, exch, routingKey);
            stageMessage = "initialising video capture system";
            java.util.Iterator frameIterator = buildFrameIterator(desiredWidth,
								  desiredHeight,
								  targetFrameRate);
            stageMessage = "during video capture";
            sender.sendFrames(targetFrameRate, frameIterator);
        } catch (Exception e) {
            SwingUtil.complainFatal("Error " + stageMessage,
                                    "Error " + stageMessage,
                                    e);
        }
    }

    public void openControlWindow(String host, String exch, String routingKey) {
        JPanel p = new JPanel();
        p.setLayout(new java.awt.GridLayout(4, 1));

        p.add(new JLabel("Host: " + host));
        p.add(new JLabel("Channel Name: " + exch));
        p.add(new JLabel("Nickname: " + routingKey));
        p.add(new JLabel("Close this window to stop capturing"));

        JFrame f = new JFrame("RabbitCam: Capture Control");
        f.getContentPane().add(p);
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.show();
    }

    public abstract java.util.Iterator buildFrameIterator(int desiredWidth,
							  int desiredHeight,
							  int targetFrameRate)
        throws Exception;
}
