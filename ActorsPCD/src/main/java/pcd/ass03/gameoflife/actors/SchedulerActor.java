package pcd.ass03.gameoflife.actors;

import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.Duration;

public class SchedulerActor extends AbstractActor {

	private final long rate;
	private final ActorRef subscriber;
	private Cancellable refreshSchedule;

	private final LoggingAdapter log;
	private Receive stoppedBehavior;
	private Receive playingBehavior;
	
	public static final class StartSchedulerMsg { }
	
	public static final class StopSchedulerMsg { }
	
	public static final class TickMsg { }
	
	public static final class ChangeRateMsg {
		private final int rate;
		
		public ChangeRateMsg(final int rate) {
			this.rate = rate;
		}
		
		public int getRate() {
			return this.rate;
		}
	}

	public static Props props(final long rate, final ActorRef subscriber) {
		return Props.create(SchedulerActor.class, rate, subscriber);
	}

	public SchedulerActor(final long rate, final ActorRef subscriber) {
		this.rate = rate;
		this.subscriber = subscriber;

		this.log = Logging.getLogger(getContext().getSystem(), this);

		this.stoppedBehavior = receiveBuilder()
				.match(StartSchedulerMsg.class, msg -> {
					createScheduledRefresh(this.rate);
					getContext().become(this.playingBehavior);
				})
				.match(ChangeRateMsg.class, msg -> {
					createScheduledRefresh(msg.getRate());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(StopSchedulerMsg.class, msg -> {
					this.refreshSchedule.cancel();
					getContext().become(this.stoppedBehavior);
				})
				.match(ChangeRateMsg.class, msg -> {
					this.refreshSchedule.cancel();
					createScheduledRefresh(msg.getRate());
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}

	private void createScheduledRefresh(final long refreshRate) {
		this.refreshSchedule = getContext().getSystem().scheduler().schedule(
				Duration.Zero(),
				Duration.create(refreshRate, TimeUnit.MILLISECONDS),
				() -> this.subscriber.tell(new TickMsg(), ActorRef.noSender()),
				getContext().system().dispatcher());
	}

	@Override
	public Receive createReceive() {
		return this.stoppedBehavior;
	}

}