package pcd.ass03.chat.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.messages.BroadcastMsg;
import pcd.ass03.chat.messages.ClientMsg;
import pcd.ass03.chat.messages.SequenceNumberClientMsg;
import pcd.ass03.chat.messages.TimestampClientMsg;
import pcd.ass03.chat.messages.client.LoggedInClientsMsg;
import pcd.ass03.chat.messages.client.LoggedOutClientMsg;
import pcd.ass03.chat.messages.client.LostMutualExclusionAfterLogoutMsg;
import pcd.ass03.chat.messages.client.NewLoggedInClientMsg;
import pcd.ass03.chat.messages.register.ClientLoginMsg;
import pcd.ass03.chat.messages.register.ClientLogoutMsg;
import pcd.ass03.chat.messages.register.LoggedOutWithMutualExclusionMsg;

/**
 * This actor represents the chat register.
 * It is known by all clients at start.
 *
 */
public class RegisterActor extends AbstractActor {

	private final Map<ActorRef, String> clientsRefs;

	private int currentMessageId;

	private final Map<ClientMsg, List<Integer>> received;
	private final Map<ClientMsg, Set<ActorRef>> recipients;
	
	private final LoggingAdapter log;
	
	
	/**
	 * Creates Props for a register actor.
	 * 
	 * @return a Props for creating register actor, which can then be further configured
	 */
	public static Props props() {
		return Props.create(RegisterActor.class);
	}
	
	/**
	 * Creates a register actor.
	 */
	public RegisterActor() {
		this.clientsRefs = new HashMap<>();
		
		this.recipients = new HashMap<>();
		this.received = new HashMap<>();
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		System.out.println("Waiting for clients to join...");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// A new client has just logged in!
				.match(ClientLoginMsg.class, loginMsg -> {
					// Tells all the actors that there is a new joined client
					sendToAll(new NewLoggedInClientMsg(loginMsg.getClientRef(), loginMsg.getUsername()));
					
					// Registers the new arrival
					this.clientsRefs.put(loginMsg.getClientRef(), loginMsg.getUsername());
					// Watches the new client actor for dying connection or disconnect
					getContext().watch(loginMsg.getClientRef());
					// Tells the joined client actor about all existing ones
					loginMsg.getClientRef().tell(new LoggedInClientsMsg(this.clientsRefs), ActorRef.noSender());
					
					final StringBuilder builder = new StringBuilder();
					builder.append("\n[IN] New client connected: " + loginMsg.getClientRef() + "(" + loginMsg.getUsername() +")");
					builder.append("\nClients connected:");
					this.clientsRefs.forEach((clientRef, username) -> { builder.append("\n" + clientRef + "(" + username + ")"); });
					builder.append("\n\n");
					System.out.println(builder.toString());
				})
				// Received a time stamped message as acknowledge
				.match(TimestampClientMsg.class, msg -> {
					// Registers the received acknowledge
					this.received.get(msg.getMessage()).add(msg.getLogicalTime());
					// Checks completion and eventually computes sequence number
					computeSequenceNumber(msg.getMessage());
				})
				// A client has just logged out!
				.match(ClientLogoutMsg.class, logoutMsg -> {
					removeClient(logoutMsg.getClientRef());
					System.out.println(logoutMsg.getClientRef() + " has logged out");
				})
				// A remote client died (gracefully termination or lost association due to network failure or crashes)  
				.match(Terminated.class, terminatedMsg -> {
					removeClient(terminatedMsg.getActor());
					this.recipients.entrySet().forEach(entry -> {
						if (entry.getValue().contains(terminatedMsg.getActor())) {
							entry.getValue().remove(terminatedMsg.getActor());
							computeSequenceNumber(entry.getKey());
						}
					});
					System.out.println(terminatedMsg.getActor() + " has died");
				})
				.match(LoggedOutWithMutualExclusionMsg.class, logoutCsMsg -> {
					sendToAll(new LostMutualExclusionAfterLogoutMsg(logoutCsMsg.getClientUsername()));
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}
	
	private void sendToAll(final BroadcastMsg broadcastMessage) {
		// Broadcasts the message
		final Set<ActorRef> currentClientsRefs = new HashSet<>(this.clientsRefs.keySet());
		final ClientMsg broadcastMsg = new ClientMsg(getSelf(), this.currentMessageId++, broadcastMessage);
		currentClientsRefs.forEach(clientRef -> clientRef.tell(broadcastMsg, ActorRef.noSender()));
		// Stores the recipients of the message
		this.recipients.put(broadcastMsg, currentClientsRefs);
		// Prepares the set in which to put the logical times of the recipients
		this.received.put(broadcastMsg, new ArrayList<>());
	}
	
	/*
	 * Checks if all the time-stamped acknowledges have been received for the specified broadcast message.
	 * If so, calculates the sequence number for the message and tells it to the message recipients.
	 */
	private void computeSequenceNumber(final ClientMsg message) {
		// If all acknowledge messages have been received
		if (this.received.get(message).size() == this.recipients.get(message).size()) {
			// Picks the max clock value received as message number
			final int sequenceNumber = Collections.max(this.received.get(message));
			// Notifies message number
			this.recipients.get(message).forEach(clientRef -> {
				clientRef.tell(new SequenceNumberClientMsg(message, sequenceNumber), ActorRef.noSender());
			});
		}
	}
	
	/*
	 * Removes the specified client from the internal references and updates all existing actors.
	 * 
	 * @param clientRef
	 * 		the client actor that has left (or is died in some way)
	 */
	private void removeClient(final ActorRef clientRef) {
		// Removes the actor from the logged client into the chat
		this.clientsRefs.remove(clientRef);
		
		// Tells all remaining logged clients that someone has left
		sendToAll(new LoggedOutClientMsg(clientRef));
		
		final StringBuilder builder = new StringBuilder();
		builder.append("[OUT] New client disconnected: " + clientRef);
		builder.append("\nClients connected:");
		this.clientsRefs.forEach((actor, username) -> { builder.append("\n" + actor + "(" + username + ")"); });
		builder.append("\n");
		System.out.println(builder.toString());
	}

}
