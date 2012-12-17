package se.ltu.M7017E.lab3.audio;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.gstreamer.Caps;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.Pipeline;

/**
 * GStreamer pipeline for the receiving part. Can manage several multicast (for
 * rooms) and one unicast channel at the same time.
 */
public class Receiver extends Pipeline {

	private Element udpSource;
	// private final Element sink = ElementFactory.make("autoaudiosink", null);

	private Pipeline me = this;

	// THE UnicastReceiver to talk with someone

	public Receiver(String receiver, String sender) {

		final Element udpSource = ElementFactory.make("udpsrc", null);
		SimpleDateFormat filenameFormatter = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");
		Date date = new Date();
		String stringDate = filenameFormatter.format(date);
		System.out.println("stringDate: " + stringDate);
		final Element rtpDepay = ElementFactory.make("rtpspeexdepay", null);
		udpSource.set("port", 5003);
		successOrDie("caps",
				udpSource.getStaticPad("src").setCaps(
						Caps.fromString("application/x-rtp,"
								+ "media=(string)audio,"
								+ "clock-rate=(int)16000,"
								+ "encoding-name=(string)SPEEX, "
								+ "encoding-params=(string)1, "
								+ "payload=(int)110")));

		final Element rtpBin = ElementFactory.make("gstrtpbin", null);
		final Element sink = ElementFactory.make("filesink", null);
		sink.set("location", receiver + "/" + sender + "-" + stringDate
				+ ".ogg");
		final Element oggmux = ElementFactory.make("oggmux", null);
		final Element speexdec = ElementFactory.make("speexdec", null);
		final Element speexenc = ElementFactory.make("speexenc", null);

		// ####################### CONNECT EVENTS ######################"
		rtpBin.connect(new Element.PAD_ADDED() {
			public void padAdded(Element element, Pad pad) {
				System.out.println("Pad added: " + pad);
				if (pad.getName().startsWith("recv_rtp_src")) {
					System.out.println("\nGot new sound input pad: " + pad);

					// add them
					me.add(rtpDepay);

					// sync them
					rtpDepay.syncStateWithParent();

					// link them
					successOrDie("rtpDepay-speexdec",
							Element.linkMany(rtpDepay, speexdec));
					successOrDie("speexdec-speexenc",
							Element.linkMany(speexdec, speexenc));

					successOrDie("speexenc-oggmux",
							Element.linkMany(speexenc, oggmux));

					successOrDie(
							"bin-decoder",
							pad.link(rtpDepay.getStaticPad("sink")).equals(
									PadLinkReturn.OK));

				}
			}
		});

		// ############## ADD THEM TO PIPELINE ####################
		addMany(udpSource, rtpBin, speexdec, speexenc, oggmux, sink);

		// ###################### LINK THEM ##########################

		Pad pad = rtpBin.getRequestPad("recv_rtp_sink_0");

		successOrDie("oggmux-sink", Element.linkMany(oggmux, sink));
		successOrDie("udpSource-rtpbin", udpSource.getStaticPad("src")
				.link(pad).equals(PadLinkReturn.OK));

	}

	private static void successOrDie(String message, boolean result) {
		if (!result) {
			System.err.println("Die because of " + message);
			System.exit(-1);
		} else
			System.out.println(message + " ok");
	}

}
