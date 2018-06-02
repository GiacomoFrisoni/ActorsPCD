package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.view.ViewDataManager;

/**
 * This actor represents a client logged in the chat.
 * The message broadcasting is based on the Skeen's algorithm, in order to guarantee Total Ordering.
 *
 */
public class ClientActor extends AbstractActor {

	private final ActorSelection registerRef;
	
	private final String username;
	private final Map<ActorRef, String> clients;
	private int clock;
	private final Map<ClientMsg, Set<Integer>> received;
	private final Map<ClientMsg, Integer> pending;
	private final Map<ClientMsg, Integer> delivering;
	private final Map<ClientMsg, Set<ActorRef>> recipients;
	private int currentMessageId;
	
	private final LoggingAdapter log;
	
	
	/**
	 * Message sent from the register to the client when a new client joins the chat.</br>
	 * With this message the client will update its internal references.
	 */
	public static final class NewLoggedInClientMsg implements Serializable {
		
		private static final long serialVersionUID = 4603934889644165530L;
		
		private final ActorRef clientRef;
		private final String username;
		
		public NewLoggedInClientMsg(final ActorRef clientRef, final String username) {
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
		 * @return the username of the client actor that wants to join the chat
		 */
		public String getUsername() {
			return this.username;
		}	
	}

	/**
	 * Message sent from the register to a new client when it joins the chat.</br>
	 * With this message the client will set all its internal references to the clients logged
	 * into chat service (included itself).
	 */
	public static final class LoggedInClientsMsg implements Serializable {
		
		private static final long serialVersionUID = 9188409704306424081L;
		
		private final Map<ActorRef, String> clientRefs;
		
		public LoggedInClientsMsg(final Map<ActorRef, String> clientRefs) {
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
	 * With this message the client will delete its references to the logged out client.
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
	 * <i>No sender username is needed since all other clients know it already internally.</br>
	 * The id of a message is composed by the sender reference and an internal number
	 * determined by it.</i>
	 */
	public static final class ClientMsg implements Serializable {
		
		private static final long serialVersionUID = 3264368841428605786L;
		
		private final ActorRef sender;
		private final int messageId;
		private final String content;
		
		public ClientMsg(final ActorRef sender, final int messageId, final String content) {
			this.sender = sender;
			this.messageId = messageId;
			this.content = content;
		}
		
		/**
		 * @return the reference to the sender of the message
		 */
		public ActorRef getSender() {
			return this.sender;
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
	 * Message sent from a client as acknowledge for the broadcast one originally sent.
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
		 * @return the message to which it refers
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
		this.clients = new HashMap<>();
		this.clock = 0;
		this.received = new HashMap<>();
		this.pending = new HashMap<>();
		this.delivering = new HashMap<>();
		this.recipients = new HashMap<>();
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
					final Set<ActorRef> currentClientsRefs = this.clients.keySet();
					final ClientMsg broadcastMsg = new ClientMsg(getSelf(), this.currentMessageId++, msg.getContent());
					currentClientsRefs.forEach(clientRef -> clientRef.tell(broadcastMsg, ActorRef.noSender()));
					// Stores the recipients of the message
					this.recipients.put(broadcastMsg, currentClientsRefs);
					// Prepares the set in which to put the logical times of the recipients
					this.received.put(broadcastMsg, new HashSet<>());
				})
				// Received a new chat message (sent with broadcast mode) from a client
				.match(ClientMsg.class, msg -> {
					// Updates the logical clock value
					this.clock++;
					// Puts the message in the pending buffer
					this.pending.put(msg, this.clock);
					// Replies with current logical clock value
					msg.getSender().tell(new TimestampClientMsg(msg, this.clock), ActorRef.noSender());
				})
				// Received a time stamped message as acknowledge
				.match(TimestampClientMsg.class, msg -> {
					// Registers the received acknowledge
					this.received.get(msg.getMessage()).add(msg.getLogicalTime());
					// Checks completion and eventually computes sequence number
					computeSequenceNumber(msg.getMessage());
				})
				// Received a message number notification from a client
				.match(SequenceNumberClientMsg.class, msg -> {
					if (this.pending.containsKey(msg.getMessage())) {
						this.clock = Math.max(this.clock, msg.getSequenceNumber());
						this.pending.remove(msg.getMessage());
						this.delivering.put(msg.getMessage(), msg.getSequenceNumber());
						// Checks if the message is already deliverable
						if (deliver(msg.getMessage(), msg.getSequenceNumber())) {
							// If so, also checks if the stored delivering messages can now be delivered
							this.delivering.entrySet().stream()
								.sorted(Map.Entry.comparingByValue())
								.forEach(entry -> deliver(entry.getKey(), entry.getValue()));
						}
					}
				})
				// I'm a new logged client, register is telling me all the client actors logged into chat
				.match(LoggedInClientsMsg.class, loggedInClientsMsg -> {
					this.clients.clear();
					this.clients.putAll(loggedInClientsMsg.getClientsRefs());
					loggedInClientsMsg.getClientsRefs().values().forEach(username -> ViewDataManager.getInstance().addClient(username));
				})
				// Register is informing me that a new client is joining the chat!
				.match(NewLoggedInClientMsg.class, loggedInClientMsg -> {
					// Adds the logged in client into the clients list
					this.clients.put(loggedInClientMsg.getClientRef(), loggedInClientMsg.getUsername());
					ViewDataManager.getInstance().addClient(loggedInClientMsg.getUsername());
				})
				// Register is informing me that a client has left the chat!
				.match(LoggedOutClientMsg.class, loggedOutClientMsg -> {
					/*
					 * Removes the logged out client from the clients list, in order to
					 * not send a broadcast message to it in a future sending.
					 */
					this.clients.remove(loggedOutClientMsg.getClientRef());
					/*
					 * Removes, if present, all the pending messages with the logged out client as sender.
					 * In fact, if the logged out client was a coordinator, the messages sent by him while
					 * still pending will never be able to be delivered since it will no longer be possible
					 * to know their sequence number.
					 */
					this.pending.entrySet().removeIf(entry -> entry.getKey().getSender().equals(loggedOutClientMsg.getClientRef()));
					/*
					 * Removes the logged out client from the recipients of the messages that concerned it.
					 * In fact, if the logged out client was the recipient of a message, the coordinator will
					 * not have to wait for its acknowledge and send the sequence number only to the remaining
					 * message recipients in the chat.
					 */
					this.recipients.entrySet().forEach(entry -> {
						if (entry.getValue().contains(loggedOutClientMsg.getClientRef())) {
							entry.getValue().remove(loggedOutClientMsg.getClientRef());
							computeSequenceNumber(entry.getKey());
						}
					});
					ViewDataManager.getInstance().removeClient(this.clients.get(loggedOutClientMsg.getClientRef()));
				})
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
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
	 * Checks if the specified message is deliverable, according to its sequence number.
	 * If so, delivers it and removes the references no longer needed.
	 */
	private boolean deliver(final ClientMsg message, final int messageSequenceNumber) {
		// Gets the stored messages as a union of pending and delivering messages
		final Map<ClientMsg, Integer> storedMessages = new HashMap<>();
		storedMessages.putAll(this.pending);
		storedMessages.putAll(this.delivering);
		/*
		 * Checks if all the messages different to the one specified have a sequence number
		 * greater than its. If so, it is the turn of the specified message and it can therefore
		 * be delivered.
		 */
		if (storedMessages.entrySet().stream()
				.filter(entry -> !entry.getKey().equals(message))
				.allMatch(entry -> entry.getValue() > messageSequenceNumber)) {
			this.delivering.remove(message);
			this.recipients.remove(message);
			// Shows the message
			ViewDataManager.getInstance().addMessage(this.clients.get(message.getSender()), message.getContent());
			return true;
		}
		return false;
	}

}