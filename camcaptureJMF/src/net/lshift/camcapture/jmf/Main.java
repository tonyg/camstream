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

package net.lshift.camcapture.jmf;

import java.io.IOException;

import java.util.Vector;
import java.util.Iterator;

import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.Manager;
import javax.media.MediaException;
import javax.media.MediaLocator;
import javax.media.Player;

import javax.media.control.FormatControl;
import javax.media.control.FrameGrabbingControl;

import javax.media.protocol.CaptureDevice;
import javax.media.protocol.DataSource;

import javax.media.format.VideoFormat;
import javax.media.format.RGBFormat;

import javax.media.util.BufferToImage;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import net.lshift.camcapture.CaptureBase;
import net.lshift.camcapture.SwingUtil;

public class Main extends CaptureBase {
    public static void main (String[] args) {
        try {
            new Main(args);
        } catch (Exception e) {
            SwingUtil.complainFatal("Error", null, e);
        }
    }

    public Main(String[] args)
        throws Exception
    {
        super("camcaptureJMF", args);
    }

    public MediaLocator selectedDeviceLocator = null;
    public RGBFormat selectedFormat = null;

    public java.util.Iterator buildFrameIterator(int desiredWidth,
                                                 int desiredHeight,
						 int targetFrameRate)
        throws Exception
    {
        selectDeviceAndFormat(desiredWidth, desiredHeight);
        System.out.println("Chose device: " + selectedDeviceLocator);
        System.out.println("Chose format: " + selectedFormat);

        if (selectedDeviceLocator == null ||
            selectedFormat == null) {
            throw new Exception("No device selected - is your camera plugged in? " +
                                "Does JMF recognise it?");
        }

        DataSource dataSource = Manager.createDataSource(selectedDeviceLocator);
        CaptureDevice dataSourceDev = (CaptureDevice) dataSource;
        FormatControl[] formatControls = dataSourceDev.getFormatControls();
        for (int i = 0; i < formatControls.length; i++) {
            formatControls[i].setFormat(selectedFormat);
        }
        Player player = Manager.createRealizedPlayer(dataSource);
        player.start();

        final FrameGrabbingControl grabber =
            (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");

        return new java.util.Iterator() {
                public boolean hasNext() { return true; }
                public Object next() {
                    Buffer buf = grabber.grabFrame();
                    BufferToImage bufferConverter =
                        new BufferToImage((VideoFormat) buf.getFormat());
                    BufferedImage i =
                        (BufferedImage) bufferConverter.createImage(buf);
                    return i;
                }
                public void remove() {}
            };
    }

    public void selectDeviceAndFormat(int desiredWidth, int desiredHeight) {
        Dimension desiredSize = new Dimension(desiredWidth, desiredHeight);

        Vector captureDeviceInfos = CaptureDeviceManager.getDeviceList(new RGBFormat());
        for (Iterator i = captureDeviceInfos.iterator(); i.hasNext();) {
            CaptureDeviceInfo info = (CaptureDeviceInfo) i.next();
            Format[] formats = info.getFormats();
            for (int j = 0; j < formats.length; j++) {
                Format format = formats[j];
                if (format instanceof RGBFormat) {
                    RGBFormat r = (RGBFormat) format;
                    //System.out.println("Examining: " + r);
                    if (selectedFormat == null) {
                        selectedDeviceLocator = info.getLocator();
                        selectedFormat = r;
                    }
                    if (r.getSize().equals(desiredSize) && (r.getRedMask() != 1)) {
                        selectedDeviceLocator = info.getLocator();
                        selectedFormat = r;
                    }
                    /* // Uncomment to choose highest bits-per-pixel on offer
                    if ((selectedFormat != null) &&
                        (r.getBitsPerPixel() > selectedFormat.getBitsPerPixel())) {
                        selectedDeviceLocator = info.getLocator();
                        selectedFormat = r;
                    }
                    */
                }
            }
        }
    }
}
