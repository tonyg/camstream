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

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import java.util.zip.GZIPOutputStream;

import java.awt.image.BufferedImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

import com.rabbitmq.client.AMQP.BasicProperties;

public class AMQVideoSender extends AMQPacketProducer {
    public AMQVideoSender(String host, String exchange, String routingKey)
        throws IOException
    {
	super(host, exchange, routingKey);
    }

    public void sendFrames(int targetFrameRate, java.util.Iterator frameProducer)
        throws IOException
    {
        final int protocolVersion = 3;
        final int keyframePeriod = 3; // seconds
        
        if (targetFrameRate == 0) {
            targetFrameRate = 5;
        }

        int keyFrameEvery = targetFrameRate * keyframePeriod; // frames

	BasicProperties prop =
	    new BasicProperties(AMQVideo.MIME_TYPE, null, null, new Integer(1),
				new Integer(0), null, null, null,
				null, null, null, null,
				null, null);

	resetStatistics();

        AMQVideoDecoder decoder = new AMQVideoDecoder();
        BufferedImage image = null;

	while (frameProducer.hasNext()) {
	    long frameProductionTime;

            while (((1000.0 * frameCount) /
                    ((frameProductionTime = System.currentTimeMillis())
		     - startTime)) > targetFrameRate)
	    {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ie) {}
            }

            BufferedImage nextImage = (BufferedImage) frameProducer.next();
            if (nextImage == null) {
                continue;
            }

            if ((image == null) ||
                (image.getWidth() != nextImage.getWidth()) ||
                (image.getHeight() != nextImage.getHeight())) {
                image =  new BufferedImage(nextImage.getWidth(),
                                           nextImage.getHeight(),
                                           BufferedImage.TYPE_INT_RGB);
            }

            float compressionQuality;
            char frameKind;

            if ((frameCount % keyFrameEvery) == 0) {
                copyImage(nextImage, image);
                compressionQuality = 0.4F;
                frameKind = 'I';
            } else {
                subtractImages(nextImage, decoder.getCurrentImage(), image);
		/* compressionQuality used to be set to 0.3 here, but
		 * because we're now (as of protocol version 3)
		 * gzipping, we can afford to step up to 0.4. */
                compressionQuality = 0.4F;
                frameKind = 'P';
            }

	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    byteStream.write(protocolVersion);
	    GZIPOutputStream gz = new GZIPOutputStream(byteStream);
	    DataOutputStream s = new DataOutputStream(gz);
	    s.writeLong(frameProductionTime);
            s.write((int) frameKind);
            writeCompressed(image, compressionQuality, s);
	    s.flush();
	    gz.finish();
	    gz.flush();
	    byteStream.flush();
            byte[] compressedFrame = byteStream.toByteArray();

	    publishPacket(prop, compressedFrame);

            decoder.handleFrame(compressedFrame);

	    reportStatistics("Video");
	}
    }

    public static void copyImage(BufferedImage sourceImage,
                                 BufferedImage destImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int[] pixels = new int[width * height];
        sourceImage.getRGB(0, 0, width, height, pixels, 0, width);
        destImage.setRGB(0, 0, width, height, pixels, 0, width);
    }

    public static void subtractImages(BufferedImage newImage,
                                      BufferedImage oldImage,
                                      BufferedImage deltaImage)
    {
        int width = newImage.getWidth();
        int height = newImage.getHeight();
        int[] newLine = new int[width];
        int[] oldLine = new int[width];
        int[] deltaLine = new int[width];

        for (int i = 0; i < height; i++) {
            newImage.getRGB(0, i, width, 1, newLine, 0, width);
            oldImage.getRGB(0, i, width, 1, oldLine, 0, width);
            for (int j = 0; j < width; j++) {
                deltaLine[j] = AMQVideo.kernelEncode(oldLine[j], newLine[j]);
            }
            deltaImage.setRGB(0, i, width, 1, deltaLine, 0, width);
        }
    }

    public static void writeCompressed(BufferedImage image,
                                       float compressionQuality,
                                       java.io.OutputStream outStream)
        throws IOException
    {
        ImageWriter writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpg").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(outStream);
        writer.setOutput(ios);
        ImageWriteParam iwparam = new JPEGImageWriteParam(java.util.Locale.getDefault());
        iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwparam.setCompressionQuality(compressionQuality);
        writer.write(null, new IIOImage(image, null, null), iwparam);
        ios.flush();
        writer.dispose();
        ios.close();
    }
}
