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
