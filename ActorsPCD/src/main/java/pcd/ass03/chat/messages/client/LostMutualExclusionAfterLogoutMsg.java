package pcd.ass03.chat.messages.client;

import java.io.Serializable;

import pcd.ass03.chat.messages.BroadcastMsg;

/**
 * Message sent to the clients when the actor that has mutual exclusion log out.
 */
public final class LostMutualExclusionAfterLogoutMsg implements BroadcastMsg, Serializable {
	
	private static final long serialVersionUID = -423120451638650777L;
	
	private String client;
	
	public LostMutualExclusionAfterLogoutMsg(final String client) {
		this.client = client;
	}
	
	/**
	 * @return the reference for actor that logged out and must release the mutex
	 */
	public String getClientUsername() {
		return this.client;
	}
	
}