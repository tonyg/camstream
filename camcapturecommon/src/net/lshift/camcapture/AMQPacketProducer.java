package net.lshift.camcapture;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import java.text.NumberFormat;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.Channel;

public class AMQPacketProducer {
    public String host;
    public String exchange;
    public String routingKey;

    public Connection conn;
    public Channel ch;

    public long frameCount;
    public long byteCount;
    public long startTime;

    public static final NumberFormat nf2dp = NumberFormat.getInstance();
    static {
        nf2dp.setMinimumFractionDigits(2);
        nf2dp.setMaximumFractionDigits(2);
        nf2dp.setGroupingUsed(false);
    }

    public AMQPacketProducer(String host, String exchange, String routingKey)
        throws IOException
    {
        this.host = host;
        this.exchange = exchange;
        this.routingKey = routingKey;

        ConnectionParameters p = new ConnectionParameters();
        this.conn = new ConnectionFactory(p).newConnection(host);

	this.ch = conn.createChannel();

	ch.exchangeDeclare(exchange, "fanout");
    }

    public void resetStatistics() {
	this.frameCount = 0;
	this.byteCount = 0;
	this.startTime = System.currentTimeMillis();
    }

    public void publishPacket(BasicProperties prop, byte[] packet)
	throws IOException
    {
	this.byteCount += packet.length;
	this.frameCount++;
	ch.basicPublish(this.exchange, routingKey, prop, packet);
    }

    public void reportStatistics(String label) {
	long now = System.currentTimeMillis();
	double deltaSec = (now - startTime) / 1000.0;
	if (deltaSec > 0) {
	    double rate = byteCount / deltaSec;
	    double fps = frameCount / deltaSec;
	    System.out.println(label + "#" + frameCount + ", " +
			       nf2dp.format(fps) + " fps; " +
			       byteCount + " total -> " + nf2dp.format(rate) + " bytes/sec");
	}
    }
}
