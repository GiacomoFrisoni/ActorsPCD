package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.dungeon.ReceiveTimeout;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.view.ViewDataManager;
import scala.concurrent.duration.Duration;

/**
 * This actor represents a client logged in the chat.
 * The message broadcasting is based on the Skeen's algorithm, in order to guarantee Total Ordering.
 * The mutual exclusion is handled with the Ricart Agrawala's algorithm (optimal for a distributed scenario).
 *
 */
public class ClientActor extends AbstractActorWithStash {
	
	private static final String ENTER_CS_MESSAGE = ":enter-cs";
	private static final String EXIT_CS_MESSAGE = ":exit-cs";
	private static final long CS_TIMEOUT = 10000L;

	private final ActorSelection registerRef;
	
	private final String username;
	private int nArrivedExistingClientsStates;
	private final Map<ActorRef, String> clients;
	private int clock;
	private final Map<ClientMsg, Set<Integer>> received;
	private final Map<ClientMsg, Integer> pending;
	private final Map<ClientMsg, Integer> delivering;
	private final Map<ClientMsg, Set<ActorRef>> recipients;
	
	private int currentMessageId;
	
	private int myts;
	private int nCsEnteringConsents;
	private final Set<ActorRef> pendingCsClientsRefs;
	private boolean isInCriticalSection;
	private Set<ActorRef> csConsentsRefsExpected;
	private int nCsEnteringAcks;
	private boolean isSomeoneInCriticalSection;
	
	private final LoggingAdapter log;
	private Receive initializingBehavior;
	private Receive activeBehavior;
	
	
	/**
	 * Message sent from the register to a new client when it joins the chat.</br>
	 * With this message the client will updated its internal references to the already logged
	 * clients in order to wait their states.
	 */
	public static final class LoggedInClientsMsg implements Serializable {
		
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
	
	/**
	 * Message sent from an existing client to a new one when it discovers that it has joined
	 * the chat.</br>With this message the new client will understand if there is already someone
	 * in critical section.
	 */
	public static final class ExistingClientStateMsg implements Serializable {
		
		private static final long serialVersionUID = 9188409704306424081L;
		
		private final boolean isClientInCS; 
		
		public ExistingClientStateMsg(final boolean isClientInCS) {
			this.isClientInCS = isClientInCS;
		}
		
		/**
		 * @return true if the already logged client is in critical section, false otherwise
		 */
		public boolean isClientInCriticalSection() {
			return this.isClientInCS;
		}
	}
	
	/**
	 * Marker interface to identify the messages that can be sent with broadcast approach
	 * to all chat clients.
	 */
	public static interface BroadcastMsg {}
	
	/**
	 * Message with textual content that a client wants to display in the chat.</br>
	 * <i>No sender username is needed since all other clients know it already internally.</br>
	 */
	public static class ChatMsg implements BroadcastMsg, Serializable {

		private static final long serialVersionUID = -211710011896901456L;

		private final String content;
		
		public ChatMsg(final String content) {
			this.content = content;
		}
		
		/**
		 * @return the content of the chat message
		 */
		public String getContent() {
			return this.content;
		}
	}
	
	/**
	 * Message sent from the register to a client when a new one joins the chat.</br>
	 * With this message the client will update its internal references.
	 */
	public static class NewLoggedInClientMsg implements BroadcastMsg, Serializable {

		private static final long serialVersionUID = -211710011896901456L;

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
	 * Message sent from the register to the client when one of the clients just logged out.</br>
	 * With this message the client will delete its internal references to the logged out client.
	 */
	public static class LoggedOutClientMsg implements BroadcastMsg, Serializable {

		private static final long serialVersionUID = -211710011896901456L;

		private final ActorRef clientRef;
		
		public LoggedOutClientMsg(final ActorRef clientRef) {
			this.clientRef = clientRef;
		}

		/**
		 * @return the reference to the client that has logged out from the chat
		 */
		public ActorRef getClientRef() {
			return this.clientRef;
		}
	}
	
	/**
	 * Message sent by a client to notify other clients about its entrance into a critical section.
	 */
	public static final class GotMutualExclusionMsg implements BroadcastMsg, Serializable {
		
		private static final long serialVersionUID = -423120451638650777L;
		
		private final ActorRef sender;
		
		public GotMutualExclusionMsg(final ActorRef sender) {
			this.sender = sender;
		}
		
		/**
		 * @return the reference to the sender that has obtained mutual exclusion
		 */
		public ActorRef getSender() {
			return this.sender;
		}
	}
	
	/**
	 * Message sent to the clients when the actor that has mutual exclusion lost it.
	 */
	public static final class LostMutualExclusionMsg implements BroadcastMsg, Serializable {
		
		private static final long serialVersionUID = -423120451638650777L;
		
	}
	
	/**
	 * Message sent to the client in order to start its broadcast delivering.
	 */
	public static final class BroadcastSendingRequestMsg implements Serializable {
		
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
	
	/**
	 * Message received from a client with broadcast approach in order to achieve total order
	 * delivering.</br>The id of a message is composed by the sender reference and an internal
	 * number determined by it.</i>
	 */
	public static final class ClientMsg implements Serializable {
		
		private static final long serialVersionUID = 3264368841428605786L;
		
		private final ActorRef sender;
		private final int messageId;
		private final BroadcastMsg message;
		
		public ClientMsg(final ActorRef sender, final int messageId, final BroadcastMsg message) {
			this.sender = sender;
			this.messageId = messageId;
			this.message = message;
		}
		
		/**
		 * @return the reference to the sender of the message
		 */
		public ActorRef getSender() {
			return this.sender;
		}
		
		/**
		 * @return the message to deliver
		 */
		public BroadcastMsg getMessage() {
			return this.message;
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
	 * Message sent from a client as acknowledge for the broadcast one originally sent.</br>
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
	 * Message sent from a client to the other ones in order to request them the possibility
	 * to enter a critical section. 
	 */
	public static final class MutualExclusionRequestMsg implements Serializable {

		private static final long serialVersionUID = -423120451638650777L;
		
		private final ActorRef sender;
		private final int timestamp;
		
		public MutualExclusionRequestMsg(final ActorRef sender, final int timestamp) {
			this.sender = sender;
			this.timestamp = timestamp;
		}
		
		/**
		 * @return the reference to the sender of the request message
		 */
		public ActorRef getSender() {
			return this.sender;
		}
		
		/**
		 * @return the logical time associated to the client that has made the request
		 */
		public int getTimestamp() {
			return this.timestamp;
		}
	}
	
	/**
	 * Message sent from a client in order to give consent to the entry in critical section
	 * of another client.
	 */
	public static final class MutualExclusionConsentMsg implements Serializable {
		
		private static final long serialVersionUID = -423120451638650777L;
		
	}
	
	/**
	 * Message sent by a client to confirm his awareness regarding the entry into critical
	 * section of another client.
	 */
	public static final class GotMutualExclusionAckMsg implements Serializable {
		
		private static final long serialVersionUID = -423120451638650777L;
		
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
		this.nArrivedExistingClientsStates = 0;
		this.clients = new HashMap<>();
		this.clock = 0;
		this.received = new HashMap<>();
		this.pending = new HashMap<>();
		this.delivering = new HashMap<>();
		this.recipients = new HashMap<>();
		this.currentMessageId = 0;
		
		this.myts = Integer.MAX_VALUE;
		this.nCsEnteringConsents = 0;
		this.pendingCsClientsRefs = new HashSet<>();
		this.isInCriticalSection = false;
		this.csConsentsRefsExpected = new HashSet<>();
		this.nCsEnteringAcks = 0;
		this.isSomeoneInCriticalSection = false;
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		// Starts and connects the client to the remote server
        this.registerRef = getContext().actorSelection("akka.tcp://ChatSystem@127.0.0.1:4552/user/register");
        this.registerRef.tell(new RegisterActor.ClientLoginMsg(getSelf(), this.username), ActorRef.noSender());
        
        this.initializingBehavior = receiveBuilder()
        		// I'm a new logged client, register is telling me the clients that are already logged into the chat
				.match(LoggedInClientsMsg.class, msg -> {
					System.out.println("HO RICEVUTO L'ELENCO :D");
					this.clients.clear();
					this.clients.putAll(msg.getClientRefs());
					this.clients.values().forEach(clientUsername -> ViewDataManager.getInstance().addClient(clientUsername));
					this.nArrivedExistingClientsStates = 0;
					unstashAll();
					if (this.clients.size() > 1) {
						getContext().become(receiveBuilder()
								// An already logged client is telling me its state info
								.match(ExistingClientStateMsg.class, clientMsg -> {
									this.nArrivedExistingClientsStates++;
									if (clientMsg.isClientInCriticalSection()) {
										this.isSomeoneInCriticalSection = true;
									}
									checkCompleteLogin();
								})
								.match(ClientMsg.class, clientMsg -> stash())
								.matchAny(otherMsg -> this.log.info("Received unknown message: " + otherMsg))
								.build());
					} else {
						checkCompleteLogin();
					}
				})
				.match(ExistingClientStateMsg.class, msg -> stash())
				.match(ClientMsg.class, msg -> stash())
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
		
        this.activeBehavior = receiveBuilder()
				// Received a sending request in order to start broadcast delivering
				.match(BroadcastSendingRequestMsg.class, msg -> {
					sendToAll(msg.getMessage());
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
				// Received a notification with the sequence number of a message from a client
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
				// Received a mutual exclusion entering request from another client
				.match(MutualExclusionRequestMsg.class, msg -> {
					/*
					 * - If I don't want to enter in critical section, or...
					 * - If I have made the request after it (concurrent scenario)...
					 * I provide consent immediately.
					 * Otherwise (if I am in critical section or if I have expressed the request before it),
					 * I register its willingness to enter into mutual exclusion, in order to send it consent
					 * only when I leave.
					 */
					if (msg.getTimestamp() < this.myts) {
						msg.getSender().tell(new MutualExclusionConsentMsg(), ActorRef.noSender());
					} else {
						this.pendingCsClientsRefs.add(msg.getSender());
					}
				})
				// Received a mutual exclusion entering consent from a client
				.match(MutualExclusionConsentMsg.class, msg -> {
					this.nCsEnteringConsents++;
					// If all the consents have been received, I can finally enter the critical section
					if (this.nCsEnteringConsents == this.csConsentsRefsExpected.size()) {
						this.nCsEnteringAcks = 0;
						sendToAll(new GotMutualExclusionMsg(getSelf()));
					}
				})
				// Received a message acknowledge about my entering in critical section
				.match(GotMutualExclusionAckMsg.class, msg -> {
					this.nCsEnteringAcks++;
					enterIntoCriticalSection();
				})
				.match(ReceiveTimeout.class, msg -> {
					sendToAll(new LostMutualExclusionMsg());
				})
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public Receive createReceive() {
		return this.initializingBehavior;
	}
	
	private void sendToAll(final BroadcastMsg broadcastMessage) {
		/*
		 * I consider the sending of a new chat message only if I know that there is not a client
		 * into critical section.
		 */
		if (!this.isSomeoneInCriticalSection || (this.isSomeoneInCriticalSection && this.isInCriticalSection)) {
			// Broadcasts the message
			final Set<ActorRef> currentClientsRefs = this.clients.keySet();
			final ClientMsg broadcastMsg = new ClientMsg(getSelf(), this.currentMessageId++, broadcastMessage);
			currentClientsRefs.forEach(clientRef -> clientRef.tell(broadcastMsg, ActorRef.noSender()));
			// Stores the recipients of the message
			this.recipients.put(broadcastMsg, currentClientsRefs);
			// Prepares the set in which to put the logical times of the recipients
			this.received.put(broadcastMsg, new HashSet<>());
		}
	}
	
	private void checkCompleteLogin() {
		System.out.println(this.nArrivedExistingClientsStates + " | " + this.clients.size());
		if (this.nArrivedExistingClientsStates == this.clients.size() - 1) {
			unstashAll();
			getContext().become(this.activeBehavior);
			// TODO: VIEW DATA MANAGER ENABLE SEND
		}
	}
	
	private void enterIntoCriticalSection() {
		if (this.nCsEnteringAcks == this.csConsentsRefsExpected.size()) {
			this.isInCriticalSection = true;
			getContext().setReceiveTimeout(Duration.create(CS_TIMEOUT, TimeUnit.MILLISECONDS));
		}
	}
	
	private void exitFromCriticalSection(final ActorRef sender) {
		// I consider requesting messages to exit from mutual exclusion only if I am in critical section
		if (this.isInCriticalSection && sender.equals(getSelf())) {
			// Turns the timeout off
			getContext().setReceiveTimeout(Duration.Undefined());
			// Resets the time-stamp and the state variables
			this.myts = Integer.MAX_VALUE;
			this.isInCriticalSection = false;
			this.csConsentsRefsExpected.clear();
			// Finally sends the consent to any clients in pending state to enter into mutual exclusion
			this.pendingCsClientsRefs.forEach(pending -> pending.tell(new MutualExclusionConsentMsg(), ActorRef.noSender()));
			this.pendingCsClientsRefs.clear();
		}
		this.isSomeoneInCriticalSection = false;
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
		 * be delivered (according to its type).
		 */
		if (storedMessages.entrySet().stream()
				.filter(entry -> !entry.getKey().equals(message))
				.allMatch(entry -> entry.getValue() > messageSequenceNumber)) {
			this.delivering.remove(message);
			this.recipients.remove(message);
			
			final BroadcastMsg broadcastMsg = message.getMessage();
			// A client wants to deliver a textual chat message
			if (broadcastMsg instanceof ChatMsg) {
				final ChatMsg chatMsg = (ChatMsg)broadcastMsg;
				if (chatMsg.getContent().equals(ENTER_CS_MESSAGE)) {
					/*
					 * I consider requesting messages to enter into mutual exclusion only if I have not already started
					 * critical section entrance procedure.
					 */
					if (this.myts == Integer.MAX_VALUE) {
						/*
						 * To request mutual exclusion, the client sends a time-stamped message to all other clients
						 * and then waits for consents. As long as it has not obtained the consent of everyone, not being
						 * officially still in mutual exclusion, it can continue both to send and to receive messages.
						 */
						this.myts = this.clock;
						this.csConsentsRefsExpected = this.clients.keySet().stream().filter(ref -> !ref.equals(getSelf())).collect(Collectors.toSet());
						this.csConsentsRefsExpected.forEach(ref -> {
							ref.tell(new MutualExclusionRequestMsg(getSelf(), this.myts), ActorRef.noSender());
						});
						this.nCsEnteringConsents = 0;
					}
				} else if (chatMsg.getContent().equals(EXIT_CS_MESSAGE)) {
					exitFromCriticalSection(message.getSender());
				} else {
					// Shows the message
					ViewDataManager.getInstance().addMessage(this.clients.get(message.getSender()), chatMsg.getContent());
				}
			// Register is informing me that a new client is joining the chat!
			} else if (broadcastMsg instanceof NewLoggedInClientMsg) {
				// Adds the logged-in client into the clients list
				final NewLoggedInClientMsg loginMsg = (NewLoggedInClientMsg)broadcastMsg;
				this.clients.put(loginMsg.getClientRef(), loginMsg.getUsername());
				// Shows the new client in the list of connected actors
				ViewDataManager.getInstance().addClient(loginMsg.getUsername());
				// Replies to the new logged client with its reference and its critical section state
				loginMsg.getClientRef().tell(new ExistingClientStateMsg(this.isInCriticalSection), ActorRef.noSender());
			}
			// Register is informing me that a client has left the chat!
			else if (broadcastMsg instanceof LoggedOutClientMsg) {
				final LoggedOutClientMsg logoutMsg = (LoggedOutClientMsg)broadcastMsg;
				/*
				 * Removes the logged out client from the clients list, in order to
				 * not send a broadcast message to it in a future sending.
				 */
				this.clients.remove(logoutMsg.getClientRef());
				checkCompleteLogin();
				/*
				 * Removes, if present, all the pending messages with the logged out client as sender.
				 * In fact, if the logged out client was a coordinator, the messages sent by it while
				 * still pending will never be able to be delivered since it will no longer be possible
				 * to know their sequence number.
				 */
				this.pending.entrySet().removeIf(entry -> entry.getKey().getSender().equals(logoutMsg.getClientRef()));
				/*
				 * Removes the logged out client from the recipients of the messages that concerned it.
				 * In fact, if the logged out client was the recipient of a message, the coordinator will
				 * not have to wait for its acknowledge and send the sequence number only to the remaining
				 * message recipients in the chat.
				 */
				this.recipients.entrySet().forEach(entry -> {
					if (entry.getValue().contains(logoutMsg.getClientRef())) {
						entry.getValue().remove(logoutMsg.getClientRef());
						computeSequenceNumber(entry.getKey());
					}
				});
				
				this.csConsentsRefsExpected.remove(logoutMsg.getClientRef());
				enterIntoCriticalSection();
				
				ViewDataManager.getInstance().removeClient(this.clients.get(logoutMsg.getClientRef()));
			}
			// Received a notification about the entering in critical section of a client
			else if (broadcastMsg instanceof GotMutualExclusionMsg) {
				final GotMutualExclusionMsg csEnteringMsg = (GotMutualExclusionMsg)broadcastMsg;
				this.isSomeoneInCriticalSection = true;
				if (!csEnteringMsg.getSender().equals(getSelf())) {
					csEnteringMsg.getSender().tell(new GotMutualExclusionAckMsg(), ActorRef.noSender());
				}
			} else if (broadcastMsg instanceof LostMutualExclusionMsg) {
				exitFromCriticalSection(message.getSender());
			}
			return true;
		}
		return false;
	}
	
	@Override
	public void postStop() {
		// If the logged out client has mutual exclusion, releases it.
		if (this.isInCriticalSection) {
			sendToAll(new LostMutualExclusionMsg());
		}
		// TODO: Use ViewDataManager to enable login
	}

}