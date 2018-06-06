package pcd.ass03.chat.actors;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.Duration;

/**
 * This actor represents a scheduler that periodically send a @link{TickMsg}
 * to a subscriber, at the specified frequency rate.
 * 
 */
public class SchedulerActor extends AbstractActor {

	private static final long TIMEOUT = 10000;
	
	private final ActorRef subscriber;
	private Cancellable refreshSchedule;

	private final LoggingAdapter log;
	private Receive stoppedBehavior;
	private Receive playingBehavior;
	
	
	/**
	 * This message allows to start the scheduling.
	 */
	public static final class StartSchedulerMsg { }
	
	/**
	 * This message allows to stop the scheduling.
	 */
	public static final class StopSchedulerMsg { }
	
	/**
	 * This class represents the message sent periodically.
	 */
	public static final class TimeoutMsg { }
	

	/**
	 * Creates Props for a scheduler actor.
	 * 
	 * @return a Props for creating a scheduler actor, which can then be further configured
	 */
	public static Props props(final ActorRef subscriber) {
		return Props.create(SchedulerActor.class, subscriber);
	}

	/**
	 * Creates a scheduler actor.
	 * 
	 * @param rate
	 * 		the starting frequency rate
	 * @param subscriber
	 * 		the scheduler subscriber
	 */
	public SchedulerActor(final ActorRef subscriber) {
		this.subscriber = subscriber;

		this.log = Logging.getLogger(getContext().getSystem(), this);

		this.stoppedBehavior = receiveBuilder()
				.match(StartSchedulerMsg.class, msg -> {
					createScheduledRefresh(TIMEOUT);
					getContext().become(this.playingBehavior);
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(StopSchedulerMsg.class, msg -> {
					this.refreshSchedule.cancel();
					getContext().become(this.stoppedBehavior);
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

	/*
	 * Creates a periodically scheduling activity with the specified frequency rate.
	 */
	private void createScheduledRefresh(final long frequencyRate) {
		this.refreshSchedule = getContext().getSystem().scheduler().schedule(
				Duration.create(frequencyRate, TimeUnit.MILLISECONDS),
				Duration.create(frequencyRate, TimeUnit.MILLISECONDS),
				() -> this.subscriber.tell(new TimeoutMsg(), ActorRef.noSender()),
				getContext().system().dispatcher());
	}

	@Override
	public Receive createReceive() {
		return this.stoppedBehavior;
	}

}