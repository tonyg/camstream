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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import net.lshift.camcapture.gsm.GSMDecoder;
import net.lshift.camcapture.gsm.InvalidGSMFrameException;

public class AMQAudioDecoder {
    public GSMDecoder decoder;

    public long frameProductionTime;
    public long frameNumber;

    public SourceDataLine sourceDataLine;
    public int maxAvailable;
    public int droppedCount;

    public AMQAudioDecoder(String mixerSpec)
	throws LineUnavailableException
    {
	AudioFormat audioFormat = new AudioFormat(11025, 16, 1, true, false);
	// we have already ad-hoc downsampled by a factor of 4 from 44100.
	DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                               audioFormat,
                                               32 /* frames * 2 bytes/sample = bytes */
                                               );
	sourceDataLine = (SourceDataLine) AMQAudio.selectLine(mixerSpec, info);
	sourceDataLine.open(audioFormat);
	sourceDataLine.start();
        maxAvailable = 0;
        droppedCount = 0;
    }

    public boolean handleFrame(byte[] frameData)
    {
        try {
            DataInputStream s = new DataInputStream(new ByteArrayInputStream(frameData));
	    int protocolVersion = s.read();

	    switch (protocolVersion) {
	      case AMQAudioSender.PROTOCOL_VERSION:
		  frameProductionTime = s.readLong();

		  char frameKind = (char) s.read();
		  switch (frameKind) {
		    case 'I':
			decoder = new GSMDecoder();
			break;
		    case 'P':
			break;
		    default:
			return false;
		  }

		  frameNumber = s.readLong();

		  int samplesLen;
		  while ((samplesLen = s.readInt()) > 0) {
		      byte[] encodedSamples = new byte[samplesLen];
		      if (s.read(encodedSamples) != samplesLen) {
			  return false;
		      }

		      if (decoder != null) {
			  int[] convertedSamples;
			  try {
			      convertedSamples = decoder.decode(encodedSamples);
			  } catch (InvalidGSMFrameException igfe) {
			      return false;
			  }

			  byte[] rawSamples = new byte[convertedSamples.length * 2];
			  for (int i = 0; i < convertedSamples.length; i++) {
			      rawSamples[i * 2 + 0] = (byte) (convertedSamples[i] & 0xff);
			      rawSamples[i * 2 + 1] = (byte) ((convertedSamples[i] >> 8) & 0xff);
			  }

                          // FIXME: need a better way of regaining approximate sync! %%%
                          int available = sourceDataLine.available();
                          if (available > maxAvailable) {
                              maxAvailable = available;
                          }

                          if ((maxAvailable - available) <= (0.2 * 11025 * 2)) {
                              if (droppedCount > 0) {
                                  System.out.println(frameProductionTime +
                                                     ": Dropped " + droppedCount +
                                                     " audio frames");
                                  droppedCount = 0;
                              }
                              sourceDataLine.write(rawSamples, 0, rawSamples.length);
                          } else {
                              droppedCount++;
                          }
		      }
		  }
		  return true;
	      default:
		  return false;
	    }
        } catch (IOException ioe) {
            // Invalid frame.
            return false;
        }
    }
}
