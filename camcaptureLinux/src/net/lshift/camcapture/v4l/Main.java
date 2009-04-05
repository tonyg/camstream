package net.lshift.camcapture.v4l;

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
        
    Driver d;
    public byte[] frame;
    public BufferedImage image;

    public java.util.Iterator buildFrameIterator(int desiredWidth,
                                                 int desiredHeight)
        throws Exception
    {
        d = new Driver(0);
        System.out.println(d);

        VideoPicture vp = d.getVideoPicture();
        vp.depth = 24;
        vp.palette = VideoPicture.VIDEO_PALETTE_RGB24;
        d.setVideoPicture(vp);
        vp = d.getVideoPicture();
        System.out.println(vp);

        VideoWindow win = d.getVideoWindow();
        win.width = desiredWidth;
        win.height = desiredHeight;
        d.setVideoWindow(win);
        win = d.getVideoWindow();
        System.out.println(win);

        frame = new byte[win.frameSizeBytes(vp.depth, vp.palette)];
        DataBufferByte db = new DataBufferByte(frame, frame.length);
	ColorModel colorModel =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB),
                                    false,
                                    false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
	WritableRaster raster = Raster.createInterleavedRaster(db,
                                                               win.width,
                                                               win.height,
                                                               win.width * 3,
                                                               3,
                                                               new int[] { 2, 1, 0 },
                                                               null);
	image = new BufferedImage(colorModel, raster, false, null);

        return new java.util.Iterator() {
                public boolean hasNext() { return true; }
                public Object next() {
                    d.readFrame(frame);
                    return image;
                }
                public void remove() {}
            };
    }
}
