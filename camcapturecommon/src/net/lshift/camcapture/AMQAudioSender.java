package net.lshift.camcapture;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import net.lshift.camcapture.gsm.Encoder;

import com.rabbitmq.client.AMQP.BasicProperties;

public class AMQAudioSender extends AMQPacketProducer {
    public static final int PROTOCOL_VERSION = 1;

    public static final int GSM_FRAMES_PER_RESET = 100;
    public static final int GSM_FRAMES_PER_AMQ_MESSAGE = 1;
    public static final int AMQ_FRAMES_PER_RESET =
        GSM_FRAMES_PER_RESET / GSM_FRAMES_PER_AMQ_MESSAGE;

    public TargetDataLine targetDataLine = null;

    public AMQAudioSender(String host, String exchange, String routingKey, String mixerSpec)
        throws IOException, LineUnavailableException
    {
	super(host, exchange, routingKey);

	AudioFormat audioFormat = new AudioFormat(44100, 16, 1, true, false);
	// we ad-hoc downsample by a factor of 4.
	DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                                               audioFormat,
                                               32 /* frames * 2 bytes/sample = bytes */
                                               );
        targetDataLine = (TargetDataLine) AMQAudio.selectLine(mixerSpec, info);
        targetDataLine.open(audioFormat);
    }

    public void sendFrames()
        throws IOException
    {
	BasicProperties prop =
	    new BasicProperties(AMQAudio.MIME_TYPE, null, null, new Integer(1),
				new Integer(0), null, null, null,
				null, null, null, null,
				null, null);

	targetDataLine.start();
	resetStatistics();

	byte[] rawSamples = new byte[160 * 2 * 4]; // 2 bytes per sample, 4x downsampling
	short[] convertedSamples = new short[160];
	byte[] encodedSamples = new byte[33];

	Encoder encoder = null;

	while (true) {
	    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	    DataOutputStream s = new DataOutputStream(byteStream);

	    char frameKind;
	    if ((frameCount % AMQ_FRAMES_PER_RESET) == 0) {
		frameKind = 'I';
		encoder = new Encoder();
	    } else {
		frameKind = 'P';
	    }

	    long frameProductionTime = System.currentTimeMillis();
	    s.write(PROTOCOL_VERSION);
	    s.writeLong(frameProductionTime);
	    s.write((int) frameKind);
            s.writeLong(frameCount);

	    for (int subframe = 0; subframe < GSM_FRAMES_PER_AMQ_MESSAGE; subframe++) {
		if (targetDataLine.read(rawSamples, 0, rawSamples.length) != rawSamples.length) {
		    // closed, stopped, drained, or flushed.
		    return;
		}

		for (int i = 0; i < 160; i++) {
		    convertedSamples[i] =
			(short) ((rawSamples[i * 8 + 1] << 8) |
				 (rawSamples[i * 8] & 0xff));
		}
		encoder.encode(convertedSamples, encodedSamples);

		s.writeInt(encodedSamples.length);
		s.write(encodedSamples);
	    }
	    s.writeInt(0);

	    s.flush();
	    byteStream.flush();
            byte[] compressedFrame = byteStream.toByteArray();

	    publishPacket(prop, compressedFrame);
	    if (frameKind == 'I') { reportStatistics("Audio"); }
	}
    }

    public static Thread startInThread(final String host,
                                       final String exchange,
                                       final String routingKey,
                                       final String mixerSpec)
    {
        Thread t = new Thread() {
                public void run() {
                    main(host, exchange, routingKey, mixerSpec);
                }
            };
        t.start();
        return t;
    }

    public static void main(String[] args) {
        main(args[0], args[1], args[2], args[3]);
    }

    public static void main(String host, String exchange, String routingKey, String mixerSpec) {
	try {
	    AMQAudioSender sender = new AMQAudioSender(host, exchange, routingKey, mixerSpec);
	    sender.sendFrames();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
