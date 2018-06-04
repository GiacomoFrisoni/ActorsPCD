package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import pcd.ass03.chat.messages.BroadcastMsg;


/**
 * Message sent to the client in order to start its broadcast delivering.
 */
public final class BroadcastSendingRequestMsg implements Serializable {
	
	private static final long serialVersionUID = -1802837382153879964L;
	
	private final BroadcastMsg message;
	
	public BroadcastSendingRequestMsg(final BroadcastMsg message) {
		this.message = message;
	}
	
	/**
	 * @return the message to deliver
	 */
	public BroadcastMsg getMessage() {
		return this.message;
	}
}