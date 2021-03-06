package se.ltu.M7017E.lab3;

import org.gstreamer.Bus;
import org.gstreamer.Gst;
import org.gstreamer.GstObject;

import se.ltu.M7017E.lab3.audio.Receiver;
import se.ltu.M7017E.lab3.audio.Sender;
import se.ltu.M7017E.lab3.sip.MySipListener;

/**
 * Main application-logic class with all the necessary API.
 */
public class App {
	/**
	 * Launch the application.
	 * 
	 * @param ipAddr
	 *            IP address on which this is listening
	 */
	public App(String ipAddr) {
		// initialize GSstreamer with some debug
		Gst.init("SIP Voicemail", new String[] { "--gst-debug-level=3",
				"--gst-debug-no-color" });

		// launch SIP stack
		new MySipListener(ipAddr, Config.LISTENING_PORT, this);
	}

	/**
	 * Answer the phone for a contact.
	 * 
	 * @param ip
	 *            contact's remote IP to send stream to
	 * @param port
	 *            contact's remote port to send stream to
	 * @param callee
	 *            SIP name of the person who was called
	 * @param caller
	 *            SIP name of the person who called
	 * @return automatically attributed port for incoming stream
	 */
	public int doAnswerPhone(String ip, int port, String callee, String caller) {
		// to receive the message
		final Receiver receiver = new Receiver(callee, caller);

		// to send the welcome message
		final Sender sender = new Sender(ip, port);
		sender.getBus().connect(new Bus.EOS() {
			public void endOfStream(GstObject source) {
				/*
				 * when the welcome message has been fully played, launch
				 * recording of message
				 */
				sender.stop();
				receiver.play();
			}
		});
		// play welcome message
		sender.play();

		return receiver.getPort();
	}
}
