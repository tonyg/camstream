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

package net.lshift.camcapture.v4l2;

import java.io.Console;
import java.io.IOException;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.RGBFrameGrabber;
import au.edu.jcu.v4l4j.VideoDevice;

import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;
import net.lshift.camcapture.CaptureBase;
import net.lshift.camcapture.SwingUtil;

public class Main extends CaptureBase {
    public static void main(String[] args) {
	try {
            new Main(args);
	} catch (Exception e) {
            SwingUtil.complainFatal("Error", null, e);
	}
    }

    public Main(String[] args)
        throws Exception
    {
        super("camcapturev4l", args);
    }
        
    public java.util.Iterator buildFrameIterator(int desiredWidth,
                                                 int desiredHeight,
                                                 int targetFrameRate)
        throws Exception
    {
        final BlockingQueue<VideoFrame> frames = new ArrayBlockingQueue<VideoFrame>(1);

        VideoDevice d = new VideoDevice("/dev/video0");
        System.out.println(d);

        RGBFrameGrabber grabber = d.getRGBFrameGrabber(desiredWidth, desiredHeight, 0, 0);
        grabber.setFrameInterval(1, targetFrameRate);
	System.out.println("Number of buffered frames: " + grabber.getNumberOfVideoFrames());
        
        grabber.setCaptureCallback(new CaptureCallback() {
            @Override
            public void nextFrame(VideoFrame frame) {
		if (!frames.offer(frame)) {
		    frame.recycle();
		}
            }

            @Override
            public void exceptionReceived(V4L4JException e) {
                e.printStackTrace();
                System.exit(0);
            }
        });
        grabber.startCapture();

        return new java.util.Iterator() {
	    public VideoFrame previous = null;
	    public boolean hasNext() { return true; }
	    public Object next() {
		try {
		    if (previous != null) previous.recycle();
		    previous = frames.take();
		    return previous.getBufferedImage();
		} catch (InterruptedException ie) {
		    throw new RuntimeException(ie);
		}
	    }
	    public void remove() {}
	};
    }
}
