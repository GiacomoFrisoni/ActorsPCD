package pcd.ass03.chat.actors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.chat.messages.BroadcastMsg;
import pcd.ass03.chat.messages.ClientMsg;
import pcd.ass03.chat.messages.SequenceNumberClientMsg;
import pcd.ass03.chat.messages.TimestampClientMsg;
import pcd.ass03.chat.messages.client.BroadcastSendingRequestMsg;
import pcd.ass03.chat.messages.client.ChatMsg;
import pcd.ass03.chat.messages.client.ExistingClientStateMsg;
import pcd.ass03.chat.messages.client.GotMutualExclusionAckMsg;
import pcd.ass03.chat.messages.client.GotMutualExclusionMsg;
import pcd.ass03.chat.messages.client.LoggedInClientsMsg;
import pcd.ass03.chat.messages.client.LoggedOutClientMsg;
import pcd.ass03.chat.messages.client.LostMutualExclusionAfterLogoutMsg;
import pcd.ass03.chat.messages.client.LostMutualExclusionMsg;
import pcd.ass03.chat.messages.client.MutualExclusionConsentMsg;
import pcd.ass03.chat.messages.client.MutualExclusionRequestMsg;
import pcd.ass03.chat.messages.client.NewLoggedInClientMsg;
import pcd.ass03.chat.messages.register.ClientLoginMsg;
import pcd.ass03.chat.messages.register.LoggedOutWithMutualExclusionMsg;
import pcd.ass03.chat.view.ViewDataManager;
import pcd.ass03.chat.view.ViewDataManager.MessageType;
import scala.concurrent.duration.Duration;

/**
 * This actor represents a client logged in the chat.
 * The message broadcasting is based on the Skeen's algorithm, in order to guarantee Total Ordering.
 * The mutual exclusion is handled with the Ricart Agrawala's algorithm (optimal for a distributed scenario).
 *
 */
public class ClientActor extends AbstractActorWithStash {
	
	public enum TimeoutMode {
		SCHEDULED_TIMEOUT, INACTIVITY_TIMEOUT
	}
	
	private static final String ENTER_CS_MESSAGE = ":enter-cs";
	private static final String EXIT_CS_MESSAGE = ":exit-cs";
	private static final long CS_TIMEOUT = 10000L;
	
	private static final TimeoutMode TIMEOUT_MODE = TimeoutMode.SCHEDULED_TIMEOUT;

	private final ActorSelection registerRef;
	private ActorRef scheduler;
	
	private final String username;
	private int nArrivedExistingClientsStates;
	private final Map<ActorRef, String> clients;
	private int clock;
	private final Map<ClientMsg, List<Integer>> received;
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
        this.registerRef.tell(new ClientLoginMsg(getSelf(), this.username), ActorRef.noSender());
        
        this.initializingBehavior = receiveBuilder()
        		// I'm a new logged client, register is telling me the clients that are already logged into the chat
				.match(LoggedInClientsMsg.class, msg -> {
					this.clients.clear();
					this.clients.putAll(msg.getClientRefs());
					this.clients.values().forEach(clientUsername -> ViewDataManager.getInstance().addClient(clientUsername));
					this.nArrivedExistingClientsStates = 0;
					unstashAll();
					if (this.clients.size() > 1) {
						getContext().become(receiveBuilder()
								// An already logged client is telling me its state info
								.match(ExistingClientStateMsg.class, clientMsg -> {
									//Check if I expected this client ack
									if (this.clients.containsKey(clientMsg.getSender())) {
										this.nArrivedExistingClientsStates++;
										if (clientMsg.isClientInCriticalSection()) {
											this.isSomeoneInCriticalSection = true;
										}
										checkLoginCompletion();
									}
								})
								.match(ClientMsg.class, clientMsg -> {
									if (clientMsg.getMessage() instanceof LoggedOutClientMsg) {
										final LoggedOutClientMsg logoutMsg = (LoggedOutClientMsg)clientMsg.getMessage();
										ViewDataManager.getInstance().removeClient(this.clients.get(logoutMsg.getClientRef()));
										this.clients.remove(logoutMsg.getClientRef());
										checkLoginCompletion();
									} else {
										stash();
									}
								})
								.matchAny(otherMsg -> this.log.info("Received unknown message: " + otherMsg))
								.build());
					} else {
						checkLoginCompletion();
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
					checkCriticalSectionConsenses();
				})
				// Received a message acknowledge about my entering in critical section
				.match(GotMutualExclusionAckMsg.class, msg -> {
					this.nCsEnteringAcks++;
					checkCriticalSectionEntrance();
				})
				// Received a timeout expired notification for a too long mutual exclusion possession
				.match(ReceiveTimeout.class, msg -> {
					// Turns the timeout off
					getContext().setReceiveTimeout(Duration.Undefined());
					// Tells to all the lost of the mutual exclusion from the current client
					sendToAll(new LostMutualExclusionMsg());
				})
				// Received a timeout from scheduler, I'm too long in cs
				.match(SchedulerActor.TimeoutMsg.class, msg -> {
					// Turns the timeout off
					this.scheduler.tell(new SchedulerActor.StopSchedulerMsg(), ActorRef.noSender());
					// Tells to all the lost of the mutual exclusion from the current client
					sendToAll(new LostMutualExclusionMsg());
				})
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public Receive createReceive() {
		return this.initializingBehavior;
	}
	
	/*
	 * Sends the specified message with broadcast delivering.
	 */
	private void sendToAll(final BroadcastMsg broadcastMessage) {
		/*
		 * I consider the sending of a new chat message only if I know that there is not a client
		 * into critical section.
		 */
		if (!this.isSomeoneInCriticalSection || (this.isSomeoneInCriticalSection && this.isInCriticalSection)) {
			// Broadcasts the message
			final Set<ActorRef> currentClientsRefs = new HashSet<>(this.clients.keySet());
			final ClientMsg broadcastMsg = new ClientMsg(getSelf(), this.currentMessageId++, broadcastMessage);
			currentClientsRefs.forEach(clientRef -> clientRef.tell(broadcastMsg, ActorRef.noSender()));
			// Stores the recipients of the message
			this.recipients.put(broadcastMsg, currentClientsRefs);
			// Prepares the set in which to put the logical times of the recipients
			this.received.put(broadcastMsg, new ArrayList<>());
		}
	}
	
	/*
	 * Checks if all the existing clients have notified me with their state.
	 * If so, I go in active mode.
	 */
	private void checkLoginCompletion() {
		if (this.nArrivedExistingClientsStates == this.clients.size() - 1) {
			unstashAll();
			getContext().become(this.activeBehavior);
			ViewDataManager.getInstance().setLogged(true);
		}
	}
	
	/*
	 * Checks if all the mutual exclusion entering consents have been received.
	 * If so, the current client tells the other ones about is effective entrance.
	 */
	private void checkCriticalSectionConsenses() {
		// If all the consents have been received, I can finally enter the critical section
		if (this.nCsEnteringConsents == this.csConsentsRefsExpected.size()) {
			this.nCsEnteringAcks = 0;
			sendToAll(new GotMutualExclusionMsg(getSelf()));
		}
	}
	
	/*
	 * Checks if all the clients know that I am ready to go into critical section.
	 * If so, I get mutual exclusion and the timeout starts.
	 */
	private void checkCriticalSectionEntrance() {
		if (this.myts != Integer.MAX_VALUE) {
			if (this.nCsEnteringAcks == this.csConsentsRefsExpected.size()) {
				this.isInCriticalSection = true;
				this.startTimeout();
			}
		}
	}
	
	/*
	 * Handles the mutual exclusion release for the specified client.
	 * If I was the one to have it, I reset my variables and notify who eventually is in pending state.
	 */
	private void exitFromCriticalSection(final ActorRef sender) {
		if (this.isInCriticalSection && sender.equals(getSelf())) {
			// Turns the timeout off
			this.stopTimeout();
			// Resets the time-stamp and the state variables
			this.myts = Integer.MAX_VALUE;
			this.isInCriticalSection = false;
			this.csConsentsRefsExpected.clear();
			// Finally sends the consent to any clients in pending state to enter into mutual exclusion
			this.pendingCsClientsRefs.forEach(pending -> pending.tell(new MutualExclusionConsentMsg(), ActorRef.noSender()));
			this.pendingCsClientsRefs.clear();
		}
		this.isSomeoneInCriticalSection = false;
		ViewDataManager.getInstance().addInfoMessage(this.clients.get(sender), MessageType.MUTEX_UNLOCK);
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
				// If the chat message is equal to the special command for entering into critical section
				if (chatMsg.getContent().equals(ENTER_CS_MESSAGE)) {
					/*
					 * I consider requesting messages to enter into mutual exclusion only if I have not already started
					 * critical section entrance procedure.
					 */
					if (message.getSender().equals(getSelf())) {
						if (this.myts == Integer.MAX_VALUE) {
							/*
							 * To request mutual exclusion, the client sends a time-stamped message to all other clients
							 * and then waits for consents. As long as it has not obtained the consent of everyone, not being
							 * officially still in mutual exclusion, it can continue both to send and to receive messages.
							 */
							this.myts = this.clock;
							this.csConsentsRefsExpected = this.clients.keySet().stream().filter(ref -> !ref.equals(getSelf())).collect(Collectors.toSet());
							if (this.csConsentsRefsExpected.size() > 0) {
								this.csConsentsRefsExpected.forEach(ref -> {
									ref.tell(new MutualExclusionRequestMsg(getSelf(), this.myts), ActorRef.noSender());
								});
								this.nCsEnteringConsents = 0;
							} else {
								sendToAll(new GotMutualExclusionMsg(getSelf()));
								checkCriticalSectionEntrance();
							}
						}
					}
				}
				// If the chat message is equal to the special command for exiting by critical section
				else if (chatMsg.getContent().equals(EXIT_CS_MESSAGE)) {
					exitFromCriticalSection(message.getSender());
				} else {
					// Shows the normal message
					ViewDataManager.getInstance().addMessage(this.clients.get(message.getSender()), chatMsg.getContent());
				}		
			} 
			// Register is informing me that a new client is joining the chat!
			else if (broadcastMsg instanceof NewLoggedInClientMsg) {
				// Adds the logged-in client into the clients list
				final NewLoggedInClientMsg loginMsg = (NewLoggedInClientMsg)broadcastMsg;
				this.clients.put(loginMsg.getClientRef(), loginMsg.getUsername());
				// Shows the new client in the list of connected actors
				ViewDataManager.getInstance().addClient(loginMsg.getUsername());
				ViewDataManager.getInstance().addInfoMessage(loginMsg.getUsername(), MessageType.LOGIN);
				// Replies to the new logged client with its reference and its critical section state
				loginMsg.getClientRef().tell(new ExistingClientStateMsg(getSelf(), this.isInCriticalSection), ActorRef.noSender());
			}
			// Register is informing me that a client has left the chat!
			else if (broadcastMsg instanceof LoggedOutClientMsg) {
				final LoggedOutClientMsg logoutMsg = (LoggedOutClientMsg)broadcastMsg;
				/*
				 * Deletes the logged out client from the view.
				 */
				ViewDataManager.getInstance().removeClient(this.clients.get(logoutMsg.getClientRef()));
				ViewDataManager.getInstance().addInfoMessage(this.clients.get(logoutMsg.getClientRef()), MessageType.LOGOUT);
				
				/*
				 * Removes the logged out client from the clients list, in order to
				 * not send a broadcast message to it in a future sending.
				 */
				this.clients.remove(logoutMsg.getClientRef());
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
				/*
				 * Removes the expected clients for the critical section entering acknowledges.
				 */
				this.csConsentsRefsExpected.remove(logoutMsg.getClientRef());
				checkCriticalSectionEntrance();
			}
			// Received a notification about the entering in critical section of a client
			else if (broadcastMsg instanceof GotMutualExclusionMsg) {
				final GotMutualExclusionMsg csEnteringMsg = (GotMutualExclusionMsg)broadcastMsg;
				this.isSomeoneInCriticalSection = true;
				if (!csEnteringMsg.getSender().equals(getSelf())) {
					csEnteringMsg.getSender().tell(new GotMutualExclusionAckMsg(), ActorRef.noSender());
				}
				ViewDataManager.getInstance().addInfoMessage(this.clients.get(csEnteringMsg.getSender()), MessageType.MUTEX_LOCK);
			}
			// Received a notification about the exiting from the critical section performed by a client
			else if (broadcastMsg instanceof LostMutualExclusionMsg) {
				exitFromCriticalSection(message.getSender());
			}
			// Received a notification about a client that logged out but previously was in mux section
			else if (broadcastMsg instanceof LostMutualExclusionAfterLogoutMsg) {
				this.isSomeoneInCriticalSection = false;
				final LostMutualExclusionAfterLogoutMsg lostMuxAfterLogout = (LostMutualExclusionAfterLogoutMsg) broadcastMsg;
				ViewDataManager.getInstance().addInfoMessage(lostMuxAfterLogout.getClientUsername(), MessageType.MUTEX_UNLOCK);
			}
			
			return true;
		}
		return false;
	}
	
	private void startTimeout() {
		//Check for configuration of timeout
		if (TIMEOUT_MODE.equals(TimeoutMode.INACTIVITY_TIMEOUT)) {
			getContext().setReceiveTimeout(Duration.create(CS_TIMEOUT, TimeUnit.MILLISECONDS));
		} else {
			this.scheduler.tell(new SchedulerActor.StartSchedulerMsg(), ActorRef.noSender());
		}
	}
	
	private void stopTimeout() {
		//Check for configuration of timeout
		if (TIMEOUT_MODE.equals(TimeoutMode.INACTIVITY_TIMEOUT)) {
			getContext().setReceiveTimeout(Duration.Undefined());
		} else {
			this.scheduler.tell(new SchedulerActor.StopSchedulerMsg(), ActorRef.noSender());
		}
	}
	
	@Override
	public void preStart() {
		// Creates the scheduler actor
		this.scheduler = getContext().actorOf(SchedulerActor.props(getSelf()), "scheduler");
	}
	
	@Override
	public void postStop() {
		// If the logged out client has mutual exclusion, releases it.
		if (this.isInCriticalSection) {
			exitFromCriticalSection(getSelf());
			this.registerRef.tell(new LoggedOutWithMutualExclusionMsg(this.username), ActorRef.noSender());
		}
		ViewDataManager.getInstance().setLogged(false);
	}

}