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

package net.lshift.camdisplay;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JComponent;
import javax.swing.JLabel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import net.lshift.camcapture.AMQAudio;
import net.lshift.camcapture.AMQAudioDecoder;
import net.lshift.camcapture.AMQVideo;
import net.lshift.camcapture.AMQVideoDecoder;

public class CamstreamComponent extends JComponent {
    public AMQVideoDecoder vDecoder;
    public AMQAudioDecoder aDecoder;

    public long videoFrames = 0;
    public long audioFrames = 0;
    public long otherFrames = 0;
    public JLabel statusLabel = null;

    public CamstreamComponent(String audioMixerSpec) {
	vDecoder = new AMQVideoDecoder();
        try {
            aDecoder = new AMQAudioDecoder(audioMixerSpec);
        } catch (LineUnavailableException lue) {
            aDecoder = null;
        }
    }

    public void setStatusLabel(JLabel l) {
	statusLabel = l;
    }

    /* Returns true if the frame needs repacking. */
    public boolean handleDelivery(String contentType, byte[] body)
        throws IOException
    {
        if (AMQVideo.MIME_TYPE.equals(contentType)) {
	    videoFrames++;
	    updateStatusLabel();
            if (vDecoder.handleFrame(body)) {
                return blitImage();
            }
        } else if (AMQAudio.MIME_TYPE.equals(contentType)) {
            audioFrames++;
            updateStatusLabel();
            if (aDecoder != null) {
                aDecoder.handleFrame(body);
            }
        } else {
	    otherFrames++;
	    updateStatusLabel();
	}
        return false;
    }

    public void updateStatusLabel() {
	if (statusLabel != null) {
	    statusLabel.setText(
                    "V" + videoFrames +
                    "/A" + audioFrames +
                    "/?" + otherFrames);
	}
    }

    public boolean blitImage() {
        Dimension currentSize = vDecoder.getCurrentImageSize();
        Dimension d = this.getPreferredSize();

        boolean needsRepack = false;
        if (d == null || !d.equals(currentSize)) {
            this.setPreferredSize(currentSize);
            this.revalidate();
            needsRepack = true;
        }

        this.repaint();

        return needsRepack;
    }

    public void paint(Graphics g) {
        super.paint(g);
        BufferedImage currentImage = vDecoder.getCurrentImage();
        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, this);
        }
    }
}
