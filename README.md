# Camstream

This page describes Camstream, an example AMQP client program
that demonstrates some of the features of RabbitMQ.

## Streaming video over AMQP

Camstream uses AMQP to route live, streaming video from one or more
webcams to one or more displays. A capture program injects frames into
a named AMQP fanout exchange, and a corresponding playback program
uses an anonymous auto-delete queue to retrieve a stream from a named
exchange.

<img src="img/camstream-diagram.png" alt="Camstream network diagram"/>

In this way, Camstream behaves similarly to a video-enabled
[IRC](http://en.wikipedia.org/wiki/Internet_Relay_Chat), where an
exchange models an IRC channel.

## Broker configuration

In order to run camstream on your own AMQP broker installation, you
need to configure the broker to have

  - a user `camstream` with password `camstream`,
  - a virtual host `/camstream`,
  - a mapping between the user `camstream` and the virtual host `/camstream`,
    in order to allow that user access to that particular virtual host.

Follow the instructions in the [admin
guide](http://www.rabbitmq.com/admin-guide.html) for configuring a
RabbitMQ broker. Here is transcript of the required commands:

    $ rabbitmqctl add_user camstream camstream
    Creating user "camstream" ...done.
    $ rabbitmqctl add_vhost /camstream
    Creating vhost "/camstream" ...done.
    $ rabbitmqctl map_user_vhost camstream /camstream
    Mapping user "camstream" to vhost "/camstream" ...done.

## Running Camstream

This section describes how to run Camstream from the command-line, if
you chose to download the software or compile the source code.

Camstream is made up of two separate classes of program: the *capture*
programs, for various architectures, that capture video frames and
inject them into an AMQP broker; and the *display* program, written in
pure java, that retrieves and displays video and provides a basic text
chat application.

### Running capture programs

The capture programs are camcaptureJMF, camcaptureOSX,
camcaptureLinux, in Java Media Framework, Quicktime and Video4Linux
variants respectively.

  - The JMF variant supports Windows, Linux and Solaris. You can
    download the JMF for your platform from
    [here](http://java.sun.com/products/java-media/jmf/downloads/index.html).

  - The Quicktime variant is for Mac OS X machines, running OS X
    version 10.3.9 or greater.

  - The Video4Linux variant is an alternative to the JMF variant for
    Linux machines with V4L-compatible webcams. Use this if you do not
    wish to install the JMF for Linux.

You can either start the programs without command-line arguments,
which will open a window asking for the startup parameters the program
needs:

    $ ./camcaptureJMF
    $ ./camcaptureOSX
    $ ./camcaptureLinux
    C:> camcaptureJMF.bat

or you can supply the following command-line arguments:

    ./camcapture??? hostname exchangename routingkey framerate x-res y-res

  - hostname: The host name of the AMQP server to route video through.

  - exchangename: The exchange (i.e. "channel") to publish video
    to. This is a similar notion to IRC's "channel" concept.

  - routingkey: The AMQP routing key to use for each transmitted piece
    of media. This is a similar notion to IRC's "nick" concept - a
    personal identifier for use within a channel.

  - framerate: Defaults to 5 fps. This limits the maximum number of
    video frames per second the program will capture and transmit.

  - x-res and y-res: Defaults to 176x144. These parameters configure
    the resolution of the video stream to be captured and
    transmitted. Note that if a resolution is selected that is not
    supported by the camera, it will silently ignore these settings.

### Running the playback program

The playback program is called camdisplay, and runs on any
Swing-supporting Java runtime, including those for Windows, Linux and
Mac OS X.

    ./camdisplay hostname exchangename routingkey

  - hostname: The host name of the AMQP server to retrieve video from.

  - exchangename: The exchange (i.e. "channel") to bind our AMQP queue
    to.

  - routingkey: The AMQP routing key to use for each transmitted piece
    of media. This is a similar notion to IRC's "nick" concept - a
    personal identifier for use within a channel.

Note that the framerate is entirely controlled by the intersection of
the capture program's maximum framerate and the available downstream
bandwidth between the AMQP server and the <code>camdisplay</code>
client.
