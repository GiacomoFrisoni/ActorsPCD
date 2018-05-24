package pcd.ass03.gameoflife.actors;

import java.awt.Point;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.gameoflife.messages.GenerationResultsMsg;
import pcd.ass03.gameoflife.messages.TickMsg;

public class ViewActor extends AbstractActorWithStash {
	
	private static final long DEFAULT_REFRESH_RATE_MILLIS = 2000; 
	
	private final int width;
	private final int height;
	private ActorRef scheduler;

	private int nGenerationNotShown;
	
	private final LoggingAdapter log;
	private Receive pausedBehavior;
	private Receive playingBehavior;
	
	/**
	 * Create Props for a view actor.
	 * 
	 * @return a Props for creating view actor, which can then be further configured
	 */
	public static Props props(final int width, final int height) {
		return Props.create(ViewActor.class, width, height);
	}
	
	public ViewActor(final int width, final int height) {
		this.width = width;
		this.height = height;
		
		this.nGenerationNotShown = 0;
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		this.pausedBehavior = receiveBuilder()
				.matchEquals("play", msg -> {
					this.scheduler.tell("play", ActorRef.noSender());
					unstashAll();
					getContext().become(this.playingBehavior);
				})
				.match(GenerationResultsMsg.class, msg -> stash())
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(GenerationResultsMsg.class, msg -> {
					this.nGenerationNotShown++;
					stash();
				})
				.match(TickMsg.class, refreshMsg -> {
					if (this.nGenerationNotShown > 0) {
						unstash();
						getContext().become(receiveBuilder()
								.match(GenerationResultsMsg.class, msg -> {
									// Shows results
									for (int y = 0; y < this.height; y++) {
										for (int x = 0; x < this.width; x++) {
											System.out.print(msg.getGenerationComputed().get(new Point(x, y)) ? "O " : "X ");
										}
										System.out.println();
									}
									System.out.println();
									// Updates counter
									this.nGenerationNotShown--;
									getContext().unbecome();
								})
								.matchAny(msg -> log.info("Received unknown message: " + msg))
								.build(), false);
					}
				})
				.matchEquals("pause", msg -> getContext().become(this.pausedBehavior))
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public void preStart() {
		this.scheduler = getContext().actorOf(SchedulerActor.props(DEFAULT_REFRESH_RATE_MILLIS, getSelf()), "scheduler");
	}
	
	@Override
	public Receive createReceive() {
		return this.pausedBehavior;
	}
	
}
