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

import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

public class AMQAudio {
    public static final String MIME_TYPE = "audio/x-camcapture";

    public static DataLine selectLine(String mixerSpec,
                                      Line.Info info)
        throws LineUnavailableException
    {
        if (mixerSpec == null) {
            throw new LineUnavailableException("No mixer selected");
        }

        Mixer.Info[] allMixers = AudioSystem.getMixerInfo();
        for (int i = 0; i < allMixers.length; i++) {
            Mixer.Info mi = allMixers[i];
            Mixer m = AudioSystem.getMixer(mi);
            System.out.println("Mixer [" + i + "] " + mi);
        }

        for (int i = 0; i < allMixers.length; i++) {
            Mixer.Info mi = allMixers[i];
            if (mi.toString().indexOf(mixerSpec) != -1) {
                Mixer m = AudioSystem.getMixer(mi);
                return (DataLine) m.getLine(info);
            }
        }

        throw new LineUnavailableException("No mixer or invalid mixer selected");
    }
}
