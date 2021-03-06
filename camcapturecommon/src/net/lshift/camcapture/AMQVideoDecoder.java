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

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

public class AMQVideoDecoder {
    public long frameProductionTime;
    public BufferedImage currentImage;

    public AMQVideoDecoder() {
        currentImage = null;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    public Dimension getCurrentImageSize() {
        return new Dimension(currentImage.getWidth(),
                             currentImage.getHeight());
    }

    public boolean handleFrame(byte[] frameData)
    {
        try {
	    if (frameData.length == 0) {
		return false;
	    }

	    int protocolVersion = frameData[0];
	    ByteArrayInputStream frameDataStream = new ByteArrayInputStream(frameData,
									    1,
									    frameData.length - 1);

            DataInputStream s;
            char frameKind;

            if ((protocolVersion == (int) 'I') ||
                (protocolVersion == (int) 'P')) {
                /* Old protocol, without version numbering! */
		s = new DataInputStream(frameDataStream);
                frameProductionTime = 0;
                frameKind = (char) protocolVersion;
            } else {
		/* Protocol versions 1 and 2 use the same frame
		 * format, and differ in how the P frames are
		 * encoded. Protocol version 3 uses gzip compression
		 * of each frame. */
		switch (protocolVersion) {
		  case 3:
		      s = new DataInputStream(new GZIPInputStream(frameDataStream));
		      break;
		  case 2:
		  case 1:
		      s = new DataInputStream(frameDataStream);
		      break;
		  default:
		      return false; // although it'd be pretty odd if we reach here.
		}
		frameProductionTime = s.readLong();
		frameKind = (char) s.read();
	    }

            switch (frameKind) {
              case 'I':
                  currentImage = ImageIO.read(s);
                  return true;

              case 'P': {
                  BufferedImage deltaImage = ImageIO.read(s);
                  if ((currentImage == null) ||
                      (currentImage.getWidth() != deltaImage.getWidth()) ||
                      (currentImage.getHeight() != deltaImage.getHeight())) {
                      currentImage =
                          new BufferedImage(deltaImage.getWidth(),
                                            deltaImage.getHeight(),
                                            BufferedImage.TYPE_INT_RGB);
                  }
                  BufferedImage newImage = 
                      new BufferedImage(deltaImage.getWidth(),
                                        deltaImage.getHeight(),
                                        BufferedImage.TYPE_INT_RGB);
                  switch (protocolVersion) {
		    case 3:
                    case 2:
                        combineDeltaV2(currentImage, deltaImage, newImage);
                        break;
                    case 1:
                    default:
                        combineDeltaV1(currentImage, deltaImage, newImage);
                        break;
                  }
                  currentImage = newImage;
                  return true;
              }

              default:
                  return false;
            }
        } catch (IOException ioe) {
            // Invalid frame.
            return false;
        }
    }

    public static int channelAdd(int oldPixel, int deltaPixel, int shiftAmount) {
        int newPixel = 
            (((oldPixel >> shiftAmount) & 0xff) +
             ((((deltaPixel >> shiftAmount) & 0xff) - 128) << 1));
        if (newPixel < 0) newPixel = 0;
        if (newPixel > 255) newPixel = 255;
        return newPixel;
    }

    public static void combineDeltaV1(BufferedImage oldImage,
                                      BufferedImage deltaImage,
                                      BufferedImage newImage)
    {
        int width = oldImage.getWidth();
        int height = oldImage.getHeight();
        int[] newLine = new int[width];
        int[] oldLine = new int[width];
        int[] deltaLine = new int[width];

        for (int i = 0; i < height; i++) {
            deltaImage.getRGB(0, i, width, 1, deltaLine, 0, width);
            oldImage.getRGB(0, i, width, 1, oldLine, 0, width);
            for (int j = 0; j < width; j++) {
                int deltaPixel = deltaLine[j];
                int oldPixel = oldLine[j];
                int newR = channelAdd(oldPixel, deltaPixel, 16);
                int newG = channelAdd(oldPixel, deltaPixel, 8);
                int newB = channelAdd(oldPixel, deltaPixel, 0);
                int newPixel = (newR << 16) | (newG << 8) | newB;
                newLine[j] = newPixel;
            }
            newImage.setRGB(0, i, width, 1, newLine, 0, width);
        }
    }

    public static void combineDeltaV2(BufferedImage oldImage,
                                      BufferedImage deltaImage,
                                      BufferedImage newImage)
    {
        int width = oldImage.getWidth();
        int height = oldImage.getHeight();
        int[] newLine = new int[width];
        int[] oldLine = new int[width];
        int[] deltaLine = new int[width];

        for (int i = 0; i < height; i++) {
            deltaImage.getRGB(0, i, width, 1, deltaLine, 0, width);
            oldImage.getRGB(0, i, width, 1, oldLine, 0, width);
            for (int j = 0; j < width; j++) {
                newLine[j] = AMQVideo.kernelDecode(oldLine[j], deltaLine[j]);
            }
            newImage.setRGB(0, i, width, 1, newLine, 0, width);
        }
    }
}
