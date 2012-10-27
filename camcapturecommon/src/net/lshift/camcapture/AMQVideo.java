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
