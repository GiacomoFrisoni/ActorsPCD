package pcd.ass03.chat.actors;

import java.util.HashMap;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class RegisterActor extends AbstractActor {

	private final Map<ActorRef, String> actorRefs;
	private final LoggingAdapter log;
	
	/**
	 * Message received when a new client try to join the chat
	 */
	public static final class ClientLoginMsg {
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
	public static final class ClientLogoutMsg {
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
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				
				//New client has just logged in!
				.match(ClientLoginMsg.class, loginMsg -> {
					//First tell the joined actor about all extisting ones
					loginMsg.getActorRef().tell(new ClientActor.ExistingLoggedInClientsMsg(this.actorRefs), ActorRef.noSender());
					
					//Then tell all the actors there is new joined actor
					this.actorRefs.forEach((actor, username) -> {
						actor.tell(new ClientActor.LoggedInClientMsg(loginMsg.getActorRef(), loginMsg.getUsername()), ActorRef.noSender());
					});
					
					//Finally add the new arrival to the map
					this.actorRefs.put(loginMsg.getActorRef(), loginMsg.getUsername());
				})
				
				//A client has just logged out!
				.match(ClientLogoutMsg.class, logoutMsg -> {
					//First tell all the actor there is no more this one
					this.actorRefs.forEach((actor, username) -> {
						actor.tell(new ClientActor.LoggedOutClientMsg(logoutMsg.getActorRef()), ActorRef.noSender());
					});
					
					//Then update the map
					this.actorRefs.remove(logoutMsg.getActorRef());
				})
				
				//WTF are you sending to me?
				.matchAny(msg -> log.info("Received unknown message: " + msg))
								
				.build();
	}

}
