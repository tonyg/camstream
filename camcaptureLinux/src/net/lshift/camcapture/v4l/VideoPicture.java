package net.lshift.camcapture.v4l;

public class VideoPicture implements Cloneable {
    public static final int VIDEO_PALETTE_GREY = 1;
    public static final int VIDEO_PALETTE_HI240 = 2;
    public static final int VIDEO_PALETTE_RGB565 = 3;
    public static final int VIDEO_PALETTE_RGB24 = 4;
    public static final int VIDEO_PALETTE_RGB32 = 5;
    public static final int VIDEO_PALETTE_RGB555 = 6;
    public static final int VIDEO_PALETTE_YUV422 = 7;
    public static final int VIDEO_PALETTE_YUYV = 8;
    public static final int VIDEO_PALETTE_UYVY = 9;
    public static final int VIDEO_PALETTE_YUV420 = 10;
    public static final int VIDEO_PALETTE_YUV411 = 11;
    public static final int VIDEO_PALETTE_RAW = 12;
    public static final int VIDEO_PALETTE_YUV422P = 13;
    public static final int VIDEO_PALETTE_YUV411P = 14;
    public static final int VIDEO_PALETTE_YUV420P = 15;
    public static final int VIDEO_PALETTE_YUV410P = 16;
    public static final int VIDEO_PALETTE_PLANAR = 13;
    public static final int VIDEO_PALETTE_COMPONENT = 7;

    int brightness;
    int hue;
    int colour;
    int contrast;
    int whiteness;
    int depth;
    int palette;

    public VideoPicture() {}

    public VideoPicture copy() {
        try {
            return (VideoPicture) clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    public String toString() {
        return "VideoPicture<" + brightness + "," + hue + "," + colour + "," +
            contrast + "," + whiteness + "," +
            depth + "/" + palette + ">";
    }
}
