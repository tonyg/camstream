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
