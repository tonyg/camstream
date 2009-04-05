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
