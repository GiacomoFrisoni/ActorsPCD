package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * This actor represents a chat client.
 * It is based on Skeen's algorithm in order to guarantee Total Ordering.
 *
 */
public class ClientActor extends AbstractActorWithStash {

	private final ActorSelection registerRef;
	
	private final String username;
	private final Map<ActorRef, String> clientsRefs;
	private int clock;
	private final Map<ClientMsg, Set<Integer>> received;
	private final Map<ClientMsg, Integer> pending;
	private final Map<ClientMsg, Integer> delivering;
	private int currentMessageId;
	
	private final LoggingAdapter log;
	
	
	/**
	 * Message sent from the register to the client when a new client joins the chat.</br>
	 * With this message the client will update its internal references to others with this new one.
	 */
	public static final class LoggedInClientMsg implements Serializable {
		
		private static final long serialVersionUID = 4603934889644165530L;
		
		private final ActorRef clientRef;
		private final String username;
		
		public LoggedInClientMsg(final ActorRef clientRef, final String username) {
			this.clientRef = clientRef;
			this.username = username;
		}

		/**
		 * @return the reference to the client that wants to join the chat
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}

		/**
		 * @return the username of the actor that wants to join the chat
		 */
		public String getUsername() {
			return this.username;
		}	
	}

	/**
	 * Message sent from the register to a new client when it joins the chat.</br>
	 * With this message the client will set all its internal references to clients already connected.
	 */
	public static final class ExistingLoggedInClientsMsg implements Serializable {
		
		private static final long serialVersionUID = 9188409704306424081L;
		
		private final Map<ActorRef, String> clientRefs;
		
		public ExistingLoggedInClientsMsg(final Map<ActorRef, String> clientRefs) {
			this.clientRefs = clientRefs;
		}
		
		/**
		 * @return the connected clients and their usernames
		 */
		public Map<ActorRef, String> getClientsRefs() {
			return Collections.unmodifiableMap(this.clientRefs);
		}
	}

	/**
	 * Message sent from the register to the client when one of the clients just logged out.</br>
	 * With this message the client will delete the reference to the logged out client.
	 */
	public static final class LoggedOutClientMsg implements Serializable {
		
		private static final long serialVersionUID = -2885971533834333567L;
		
		private final ActorRef clientRef;
		
		public LoggedOutClientMsg(final ActorRef clientRef) {
			this.clientRef = clientRef;
		}
		
		/**
		 * @return the reference for the logged out client
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}
	}
	
	/**
	 * Message sent to the client in order to start its delivering.
	 */
	public static final class SendingRequestMsg implements Serializable {
		
		private static final long serialVersionUID = -1802837382153879964L;
		
		private final String content;
		
		public SendingRequestMsg(final String content) {
			this.content = content;
		}
		
		/**
		 * @return the content of the message
		 */
		public String getContent() {
			return this.content;
		}
	}
	
	/**
	 * Message that a client wants to display in the chat.</br>
	 * <i>No sender username is needed since all other clients know it already internally.</i>
	 */
	public static final class ClientMsg implements Serializable {
		
		private static final long serialVersionUID = 3264368841428605786L;
		
		private final ActorRef sender;
		private final int messageId;
		private final Set<ActorRef> recipients;
		private final String content;
		
		public ClientMsg(final ActorRef sender, final int messageId, final Set<ActorRef> recipients,
				final String content) {
			this.sender = sender;
			this.messageId = messageId;
			this.recipients = recipients;
			this.content = content;
		}
		
		/**
		 * @return the reference to the sender of the message
		 */
		public ActorRef getSender() {
			return this.sender;
		}
		
		/**
		 * @return the reference to the recipients of the message
		 */
		public Set<ActorRef> getRecipients() {
			return this.recipients;
		}
		
		/**
		 * @return the message content
		 */
		public String getContent() {
			return this.content;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + messageId;
			result = prime * result + ((sender == null) ? 0 : sender.hashCode());
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof ClientMsg
					&& this.sender.equals(((ClientMsg)obj).sender)
					&& this.messageId == ((ClientMsg)obj).messageId;
		}
	}
	
	/**
	 * Reply message sent from a client as acknowledge for the broadcast one originally sent.
	 * It contains the current logical clock value of the receiver.
	 */
	public static final class TimestampClientMsg implements Serializable {

		private static final long serialVersionUID = -423120451638650777L;
		
		private final ClientMsg message;
		private final int logicalTime;
		
		public TimestampClientMsg(final ClientMsg message, final int logicalTime) {
			this.message = message;
			this.logicalTime = logicalTime;
		}
		
		/**
		 * @return the message
		 */
		public ClientMsg getMessage() {
			return this.message;
		}
		
		/**
		 * @return the client logical time associated to the message
		 */
		public int getLogicalTime() {
			return this.logicalTime;
		}
	}
	
	/**
	 * Message sent from a client in order to notify the calculated sequence number
	 * for the specified message.
	 */
	public static final class SequenceNumberClientMsg implements Serializable {

		private static final long serialVersionUID = -423120451638650777L;
		
		private final ClientMsg message;
		private final int sequenceNumber;
		
		public SequenceNumberClientMsg(final ClientMsg message, final int sequenceNumber) {
			this.message = message;
			this.sequenceNumber = sequenceNumber;
		}
		
		/**
		 * @return the message
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

	
	/**
	 * Creates Props for a client actor.
	 * 
	 * @param username
	 * 		the username of the client to be passed to the actor's constructor.
	 * @return a Props for creating client actor, which can then be further configured
	 */
	public static Props props(final String username) {
		return Props.create(ClientActor.class, username);
	}
	
	/**
	 * Creates a client actor.
	 * 
	 * @param username
	 * 		the username of the client
	 */
	public ClientActor(final String username) {
		this.username = username;
		this.clientsRefs = new HashMap<>();
		this.clock = 0;
		this.received = new HashMap<>();
		this.pending = new HashMap<>();
		this.delivering = new HashMap<>();
		this.currentMessageId = 0;
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		// Starts and connects the client to the remote server
        this.registerRef = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:4552/user/register");
        this.registerRef.tell(new RegisterActor.ClientLoginMsg(getSelf(), this.username), ActorRef.noSender());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				// Received a sending request in order to start broadcast delivering
				.match(SendingRequestMsg.class, msg -> {
					// Broadcasts the message
					final Set<ActorRef> currentClientsRefs = this.clientsRefs.keySet();
					final ClientMsg broadcastMsg = new ClientMsg(getSelf(), this.currentMessageId++, currentClientsRefs, msg.getContent());
					currentClientsRefs.forEach(clientRef -> clientRef.tell(broadcastMsg, ActorRef.noSender()));
					this.received.put(broadcastMsg, new HashSet<>());
				})
				// Received a new chat message (sent with broadcast mode) from a client
				.match(ClientMsg.class, msg -> {
					// Updates the logical clock value
					this.clock++;
					// Puts the message in the pending buffer
					this.pending.put(msg, this.clock);
					// Reply with current logical clock value
					msg.getSender().tell(new TimestampClientMsg(msg, this.clock), ActorRef.noSender());
				})
				// Received a time stamped message as acknowledge
				.match(TimestampClientMsg.class, msg -> {
					this.received.get(msg.getMessage()).add(msg.getLogicalTime());
					if (this.received.get(msg.getMessage()).size() == msg.getMessage().getRecipients().size()) {
						// Picks the max clock value received as message number
						final int sequenceNumber = Collections.max(this.received.get(msg.getMessage()));
						// Notifies message number
						msg.getMessage().getRecipients().forEach(clientRef -> {
							clientRef.tell(new SequenceNumberClientMsg(msg.getMessage(), sequenceNumber), ActorRef.noSender());
						});
					}
				})
				// Received a message number notification from a client
				.match(SequenceNumberClientMsg.class, msg -> {
					if (this.pending.containsKey(msg.getMessage())) {
						this.clock = Math.max(this.clock, msg.getSequenceNumber());
						this.pending.remove(msg.getMessage());
						this.delivering.put(msg.getMessage(), msg.getSequenceNumber());
						// Check if the message is already deliverable
						final Map<ClientMsg, Integer> union = new HashMap<>();
						union.putAll(this.pending);
						union.putAll(this.delivering);
						if (union.entrySet().stream()
								.filter(entry -> !entry.getKey().equals(msg.getMessage()))
								.allMatch(entry -> entry.getValue() > msg.getSequenceNumber())) {
							this.delivering.remove(msg.getMessage());
							// Print :D
							unstashAll();
						} else {
							stash();
						}
					}
				})
				
				
				
				
				
				// I'm a new logged client, register is telling me all the existing actors
				.match(ExistingLoggedInClientsMsg.class, existingLoggedInClientsMsg -> {
					this.clientsRefs.clear();
					this.clientsRefs.putAll(existingLoggedInClientsMsg.getClientsRefs());
					//this.knowledge.addNewClients(existingLoggedInClientsMsg.getClientsRefs().keySet(), this.clientsRefs.keySet());
				})
				// Register is informing me that new client is joining the chat!
				.match(LoggedInClientMsg.class, loggedInClientMsg -> {
					this.clientsRefs.put(loggedInClientMsg.getClientRef(), loggedInClientMsg.getUsername());
					//this.knowledge.addNewClient(loggedInClientMsg.getClientRef(), this.clientsRefs.keySet());
				})
				// Register is informing me that a client has left the chat!
				.match(LoggedOutClientMsg.class, loggedOutClientMsg -> {
					this.clientsRefs.remove(loggedOutClientMsg.getClientRef());
					//this.knowledge.removeClient(loggedOutClientMsg.getClientRef());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

}