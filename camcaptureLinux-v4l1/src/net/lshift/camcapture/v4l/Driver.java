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

public class Driver {
    static { System.loadLibrary("jv4l"); }

    public int deviceNumber;
    public int fd;

    public String deviceName;
    public int deviceType;
    public int minWidth;
    public int minHeight;
    public int maxWidth;
    public int maxHeight;

    public VideoPicture videoPicture;
    public VideoWindow videoWindow;

    public Driver(int deviceNumber)
        throws DriverException
    {
        this.deviceNumber = deviceNumber;
        this.fd = open(deviceNumber);
        if (this.fd == -1) {
            throw new DriverException("Open failed");
        }
    }

    public void close()
        throws DriverException
    {
        if (!close(fd)) {
            throw new DriverException("Close failed");
        }
    }

    public String toString() {
        return "Driver<" + deviceNumber + "=" + deviceName + "," + deviceType + "," +
            minWidth + "x" + minHeight + "-" + maxWidth + "x" + maxHeight + ">";
    }

    public VideoPicture getVideoPicture()
        throws DriverException
    {
        VideoPicture vp = new VideoPicture();
        if (!getVideoPicture(fd, vp)) {
            throw new DriverException("GetVideoPicture failed");
        }
        videoPicture = vp.copy();
        return vp;
    }

    public boolean setVideoPicture(VideoPicture vp)
        throws DriverException
    {
        if (!setVideoPicture(fd, vp))
            return false;
        getVideoPicture();
        return true;
    }

    public VideoWindow getVideoWindow()
        throws DriverException
    {
        VideoWindow vw = new VideoWindow();
        if (!getVideoWindow(fd, vw)) {
            throw new DriverException("GetVideoWindow failed");
        }
        videoWindow = vw.copy();
        return vw;
    }

    public boolean setVideoWindow(VideoWindow vw)
        throws DriverException
    {
        if (!setVideoWindow(fd, vw))
            return false;
        getVideoWindow();
        return true;
    }

    public byte[] readFrame() {
        byte[] buffer = new byte[videoWindow.frameSizeBytes(videoPicture.depth,
                                                            videoPicture.palette)];
        if (!readFrame(fd, buffer))
            return null;
        return buffer;
    }

    public boolean readFrame(byte[] buffer) {
        return readFrame(fd, buffer);
    }

    private native int open(int deviceNumber);
    private native boolean close(int fd);
    private native boolean getVideoPicture(int fd, VideoPicture vp);
    private native boolean setVideoPicture(int fd, VideoPicture vp);
    private native boolean getVideoWindow(int fd, VideoWindow vw);
    private native boolean setVideoWindow(int fd, VideoWindow vw);
    private native boolean readFrame(int fd, byte[] buffer);
}
