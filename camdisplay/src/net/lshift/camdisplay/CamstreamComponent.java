package net.lshift.camdisplay;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import net.lshift.camcapture.AMQVideo;
import net.lshift.camcapture.AMQVideoDecoder;

public class CamstreamComponent extends JComponent {
    public AMQVideoDecoder vDecoder;

    public long videoFrames = 0;
    public long otherFrames = 0;
    public JLabel statusLabel = null;

    public CamstreamComponent() {
	vDecoder = new AMQVideoDecoder();
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
        } else {
	    otherFrames++;
	    updateStatusLabel();
	}
        return false;
    }

    public void updateStatusLabel() {
	if (statusLabel != null) {
	    statusLabel.setText("V" + videoFrames + "/?" + otherFrames);
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
