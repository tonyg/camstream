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
