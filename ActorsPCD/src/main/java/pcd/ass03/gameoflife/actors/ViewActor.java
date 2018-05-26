package pcd.ass03.gameoflife.actors;

import java.awt.Point;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.gameoflife.view.View;
import pcd.ass03.gameoflife.view.ViewDataManager;

/**
 * This actor represents a view for the Conway's Game Of Life.
 * 
 */
public class ViewActor extends AbstractActor {
	
	private static final long DEFAULT_REFRESH_RATE_MILLIS = 1000;
	
	private ActorRef scheduler;
	private final View view;
	
	private final Deque<GenerationResultsMsg> generationsNotShown;
	
	private final LoggingAdapter log;
	private Receive pausedBehavior;
	private Receive playingBehavior;
	
	
	/**
	 * This message allows to start the game visualization.
	 */
	public static final class StartVisualizationMsg { }
	
	/**
	 * This message allows to stop the game visualization.
	 */
	public static final class StopVisualizationMsg { }
	
	/**
	 * This message allows to reset the game visualization.
	 */
	public static final class ResetVisualizationMsg { }
	
	/**
	 * This message contains the results of a generation that must be displayed.
	 */
	public static final class GenerationResultsMsg {
		private final int generationNumber;
		private final Map<Point, Boolean> generationComputed;
		private final long timeElapsed;
		private final long averageTime;
		private final int nAliveCells;
		
		public GenerationResultsMsg(final int generationNumber, final Map<Point, Boolean> generationComputed,
				final long timeElapsed, final long averageTime, final int nAliveCells) {
			this.generationNumber = generationNumber;
			this.generationComputed = generationComputed;
			this.timeElapsed = timeElapsed;
			this.averageTime = averageTime;
			this.nAliveCells = nAliveCells;
		}
		
		public int getGenerationNumber() {
			return this.generationNumber;
		}
		
		public Map<Point, Boolean> getGenerationComputed() {
			return this.generationComputed;
		}
		
		public long getTimeElapsed() {
			return this.timeElapsed;
		}
		
		public long getAverageTime() {
			return this.averageTime;
		}
		
		public int getNumberOfAliveCells() {
			return this.nAliveCells;
		}
	}
	
	/**
	 * This message allows to change the refresh rate for the generation results visualization.
	 */
	public static final class ChangeRefreshRateMsg {
		private final long refreshRate;
		
		public ChangeRefreshRateMsg(final long refreshRate) {
			this.refreshRate = refreshRate;
		}
		
		public long getRefreshRate() {
			return this.refreshRate;
		}
	}
	
	
	/**
	 * Creates Props for a view actor.
	 * 
	 * @return a Props for creating view actor, which can then be further configured
	 */
	public static Props props(final View view) {
		return Props.create(ViewActor.class, view);
	}
	
	/**
	 * Creates a view actor.
	 */
	public ViewActor(final View view) {
		this.view = view;
		
		this.generationsNotShown = new LinkedList<>();
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		this.pausedBehavior = receiveBuilder()
				.match(StartVisualizationMsg.class, msg -> {
					// Starts the scheduling
					this.scheduler.tell(new SchedulerActor.StartSchedulerMsg(), ActorRef.noSender());
					// Goes into playing state
					getContext().become(this.playingBehavior);
				})
				.match(ChangeRefreshRateMsg.class, msg -> {
					this.scheduler.tell(new SchedulerActor.ChangeRateMsg(msg.refreshRate), ActorRef.noSender());
				})
				.match(GenerationResultsMsg.class, msg -> this.generationsNotShown.add(msg))
				.match(ResetVisualizationMsg.class, msg -> this.generationsNotShown.clear())
				.match(SchedulerActor.TickMsg.class, msg -> { })
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(GenerationResultsMsg.class, msg -> {
					// The arrived results are managed only with the refresh frequency determined by the scheduler
					this.generationsNotShown.add(msg);
				})
				.match(SchedulerActor.TickMsg.class, refreshMsg -> {
					if (this.generationsNotShown.size() > 0) {
						final GenerationResultsMsg res = this.generationsNotShown.pop();
						// Shows results
						this.view.drawCells(res.generationComputed);
						ViewDataManager.getInstance().setGeneration(res.getGenerationNumber());
						ViewDataManager.getInstance().setAliveCells(res.getNumberOfAliveCells());
						ViewDataManager.getInstance().setElapsedTime(res.getTimeElapsed());
						ViewDataManager.getInstance().setAvgElapsedTime(res.getAverageTime());
					}
				})
				.match(ChangeRefreshRateMsg.class, msg -> {
					this.scheduler.tell(new SchedulerActor.ChangeRateMsg(msg.refreshRate), ActorRef.noSender());
				})
				.match(StopVisualizationMsg.class, msg -> {
					// Stops scheduling
					this.scheduler.tell(new SchedulerActor.StopSchedulerMsg(), ActorRef.noSender());
					// Goes into paused state
					getContext().become(this.pausedBehavior);
				})
				.match(ResetVisualizationMsg.class, msg -> this.generationsNotShown.clear())
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public void preStart() {
		// Creates the scheduler actor
		this.scheduler = getContext().actorOf(SchedulerActor.props(DEFAULT_REFRESH_RATE_MILLIS, getSelf()), "scheduler");
	}
	
	@Override
	public Receive createReceive() {
		return this.pausedBehavior;
	}
	
}
