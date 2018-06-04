package pcd.ass03.chat.messages.client;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import akka.actor.ActorRef;

/**
 * Message sent from the register to a new client when it joins the chat.</br>
 * With this message the client will updated its internal references to the already logged
 * clients in order to wait their states.
 */
public final class LoggedInClientsMsg implements Serializable {
	
	private static final long serialVersionUID = 9188409704306424081L;
	
	private final Map<ActorRef, String> clientRefs;
	
	public LoggedInClientsMsg(final Map<ActorRef, String> clientRefs) {
		this.clientRefs = clientRefs;
	}
	
	/**
	 * @return the references for the already logged clients into chat service
	 */
	public Map<ActorRef, String> getClientRefs() {
		return Collections.unmodifiableMap(this.clientRefs);
	}
}