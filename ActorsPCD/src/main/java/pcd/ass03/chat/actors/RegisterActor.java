package pcd.ass03.chat.actors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class RegisterActor extends AbstractActor {

	private final Map<ActorRef, String> actorRefs;
	private final LoggingAdapter log;
	
	/**
	 * Message received when a new client try to join the chat
	 */
	public static final class ClientLoginMsg implements Serializable {
		private static final long serialVersionUID = -9000555631707665945L;
		private final ActorRef actorRef;
		private final String username;
		
		public ClientLoginMsg (final ActorRef actorRef, final String username) {
			this.actorRef = actorRef;
			this.username = username;
		}

		/**
		 * Get the reference to actor that wants to join the chat
		 * @return
		 * 		reference to actor that wants to join the chat
		 */
		public ActorRef getActorRef() {
			return actorRef;
		}

		/**
		 * Get the username of the actor that wants to join the chat
		 * @return
		 * 		username of the actor that wants to join the chat
		 */
		public String getUsername() {
			return username;
		}	
	}

	/**
	 * Message received when a client exit the chat
	 */
	public static final class ClientLogoutMsg implements Serializable {
		private static final long serialVersionUID = -6642450939531203193L;
		private final ActorRef actorRef;
		
		public ClientLogoutMsg(final ActorRef actorRef) {
			this.actorRef = actorRef;
		}
		
		/**
		 * Get the reference to the actor that left the chat
		 * @return
		 * 		reference to the actor that left the chat
		 */
		public ActorRef getActorRef() {
			return this.actorRef;
		}
	}
	
	
	
	public static Props props() {
		return Props.create(RegisterActor.class);
	}
	
	public RegisterActor() {
		this.actorRefs = new HashMap<>();
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		System.out.println("Waiting for clients to join...");
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				
				//New client has just logged in!
				.match(ClientLoginMsg.class, loginMsg -> {
					//First tell the joined actor about all extisting ones
					loginMsg.getActorRef().tell(new ClientActor.ExistingLoggedInClientsMsg(this.actorRefs), ActorRef.noSender());
					
					//Then I watch this actor for dying connection or disconnect
					getContext().watch(loginMsg.getActorRef());
					
					//Then tell all the actors there is new joined actor
					this.actorRefs.forEach((actor, username) -> {
						actor.tell(new ClientActor.LoggedInClientMsg(loginMsg.getActorRef(), loginMsg.getUsername()), ActorRef.noSender());
					});
					
					//Finally add the new arrival to the map
					this.actorRefs.put(loginMsg.getActorRef(), loginMsg.getUsername());
					
					
					final StringBuilder builder = new StringBuilder();
					builder.append("[IN ] New client connected: " + loginMsg.getActorRef() + "(" + loginMsg.getUsername() +")");
					builder.append("\nClients connected:");
					this.actorRefs.forEach((actor, username) -> { builder.append("\n" + actor + "(" + username + ")"); });
					builder.append("\n");
					System.out.println(builder.toString());
					
				})
				
				//A client has just logged out!
				.match(ClientLogoutMsg.class, logoutMsg -> {
					removeActor(logoutMsg.getActorRef());
				})
				
				//An actor died or it connection is poor
				.match(Terminated.class, terminatedMsg -> {
					removeActor(terminatedMsg.getActor());
				})
				
				//WTF are you sending to me?
				.matchAny(msg -> log.info("Received unknown message: " + msg))
								
				.build();
	}
	
	
	/**
	 * Removes the actor from the matrix and update all existing actors there is no more the eliminated one
	 * @param actorRef
	 * 		Actor that logged out (or died in some way)
	 */
	private void removeActor(final ActorRef actorRef) {
		//First delete it from its matrix
		this.actorRefs.remove(actorRef);
		
		//Then tell all remaining actor that someone has logged out
		this.actorRefs.forEach((actor, username) -> {
			actor.tell(new ClientActor.LoggedOutClientMsg(actorRef), ActorRef.noSender());
		});
		
		final StringBuilder builder = new StringBuilder();
		builder.append("[OUT] New client disconnected: " + actorRef);
		builder.append("\nClients connected:");
		this.actorRefs.forEach((actor, username) -> { builder.append("\n" + actor + "(" + username + ")"); });
		builder.append("\n");
		System.out.println(builder.toString());
	}

}
