package net.lshift.camcapture;

public class AMQVideo {
    public static final String MIME_TYPE = "video/x-camcapture";

    public static int reducePrecision(int pixel) {
        return ((pixel >> 2) & 0x3f3f3f);
    }

    public static int clampPixel(int pixel) {
        // Clamp required because JPG occasionally sends a delta too
        // high or too low, leaving us with out-of-range pixels.
        // Clamp each channel to [40, 7F].
        if ((pixel & 0xC0C0C0) != 0x404040) {
            switch (pixel & 0xC00000) {
              case 0x000000:    pixel = (pixel & 0x00FFFF) | (0x400000); break;
              case 0x400000:    break;
              default:          pixel = (pixel & 0x00FFFF) | (0x7F0000); break;
            }
            switch (pixel & 0x00C000) {
              case 0x000000:    pixel = (pixel & 0xFF00FF) | (0x004000); break;
              case 0x004000:    break;
              default:          pixel = (pixel & 0xFF00FF) | (0x007F00); break;
            }
            switch (pixel & 0x0000C0) {
              case 0x000000:    pixel = (pixel & 0xFFFF00) | (0x000040); break;
              case 0x000040:    break;
              default:          pixel = (pixel & 0xFFFF00) | (0x00007F); break;
            }
        }
        return pixel - 0x404040;
    }

    public static int negate(int pixel) {
        return ((pixel ^ 0x3f3f3f) + 0x010101);
    }

    public static int kernelEncode(int oldPixel, int newPixel) {
        return reducePrecision(newPixel) + negate(reducePrecision(oldPixel)) + 0x404040;
    }

    public static int kernelDecode(int oldPixel, int deltaPixel) {
        return clampPixel(deltaPixel - negate(reducePrecision(oldPixel))) << 2;
    }
}
