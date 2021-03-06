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

public class VideoWindow implements Cloneable {
    public static final int VIDEO_WINDOW_INTERLACE = 1;
    public static final int VIDEO_WINDOW_CHROMAKEY = 16;
    public static final int VIDEO_CLIP_BITMAP = -1;

    int x,y;			/* Position of window */
    int width,height;		/* Its size */
    int chromakey;
    int flags;

    public VideoWindow() {}

    public VideoWindow copy() {
        try {
            return (VideoWindow) clone();
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }

    public String toString() {
        return "VideoWindow<" + x + "," + y + "," + width + "," + height + ";" +
            chromakey + "," + flags + ">";
    }

    public int frameSizeBytes(int depth, int palette)
        throws IllegalArgumentException
    {
        int pixelCount = width * height;
        int bytesPerPixel = 0;
        switch (palette) {
          case VideoPicture.VIDEO_PALETTE_GREY:
              switch (depth) {
                case 4:
                case 6:
                case 8:
                    bytesPerPixel = 1;
                    break;
                case 16:
                    bytesPerPixel = 2;
                    break;
                default:
                    break;
              }
              break;

          case VideoPicture.VIDEO_PALETTE_RGB565:
          case VideoPicture.VIDEO_PALETTE_RGB555:
              bytesPerPixel = 2;
              break;

          case VideoPicture.VIDEO_PALETTE_RGB24:
              bytesPerPixel = 3;
              break;

          case VideoPicture.VIDEO_PALETTE_RGB32:
              bytesPerPixel = 4;
              break;

          case VideoPicture.VIDEO_PALETTE_HI240:
          case VideoPicture.VIDEO_PALETTE_YUV422:
          case VideoPicture.VIDEO_PALETTE_YUYV:
          case VideoPicture.VIDEO_PALETTE_UYVY:
          case VideoPicture.VIDEO_PALETTE_YUV420:
          case VideoPicture.VIDEO_PALETTE_YUV411:
          case VideoPicture.VIDEO_PALETTE_RAW:
          case VideoPicture.VIDEO_PALETTE_YUV422P:
          case VideoPicture.VIDEO_PALETTE_YUV411P:
          case VideoPicture.VIDEO_PALETTE_YUV420P:
          case VideoPicture.VIDEO_PALETTE_YUV410P:
              break;
        }

        if (bytesPerPixel == 0) {
            throw new IllegalArgumentException("Unsupported palette/depth combination: " +
                                               palette + "/" + depth);
        }

        return bytesPerPixel * pixelCount;
    }
}
