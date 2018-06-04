package pcd.ass03.chat.messages;

import java.io.Serializable;


/**
 * Message sent from a client in order to notify the calculated sequence number
 * for the specified message.
 */
public final class SequenceNumberClientMsg implements Serializable {

	private static final long serialVersionUID = -423120451638650777L;
	
	private final ClientMsg message;
	private final int sequenceNumber;
	
	public SequenceNumberClientMsg(final ClientMsg message, final int sequenceNumber) {
		this.message = message;
		this.sequenceNumber = sequenceNumber;
	}
	
	/**
	 * @return the message to which it refers
	 */
	public ClientMsg getMessage() {
		return this.message;
	}
	
	/**
	 * @return the sequence number associated to the message
	 */
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}
}