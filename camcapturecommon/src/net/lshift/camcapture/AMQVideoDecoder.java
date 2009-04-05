package net.lshift.camcapture;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

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
            DataInputStream s = new DataInputStream(new ByteArrayInputStream(frameData));
	    int protocolVersion = s.read();
            char frameKind;

            if ((protocolVersion == (int) 'I') ||
                (protocolVersion == (int) 'P')) {
                /* Old protocol, without version numbering! */
                frameProductionTime = 0;
                frameKind = (char) protocolVersion;
            } else {
                if (protocolVersion > 2) {
                    return false;
                } else {
                    /* Protocol versions 1 and 2 use the same frame
                     * format, and differ in how the P frames are
                     * encoded. */
                    frameProductionTime = s.readLong();
                    frameKind = (char) s.read();
                }
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
