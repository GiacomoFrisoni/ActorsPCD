package pcd.ass03.gameoflife.actors;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.gameoflife.actors.CellActor.ComputeMsg;
import pcd.ass03.gameoflife.actors.CellActor.PrepareNextGenerationMsg;
import pcd.ass03.gameoflife.utilities.Chrono;
import scala.concurrent.duration.Duration;

/**
 * This actor represents a grid for the Conway's Game Of Life.
 * 
 */
public class GridActor extends AbstractActorWithStash {
	
	private int width;
	private int height;
	private ActorRef view;
	private Map<Point, ActorRef> cellsActorsMap;
	
	private boolean notYetStarted;
	private int nGenerations;
	private Map<Point, Boolean> calculatedGeneration;
	private int nAliveCells;
	private long averageTime;
	private final Chrono timer;
	private int nTerminatedCells;
	
	private final LoggingAdapter log;
	private Receive initializingBehavior;
	private Receive pausedBehavior;
	private Receive playingBehavior;
	
	
	/**
	 * This message contains the informations for the grid initialization.
	 */
	public static final class InitGridMsg {
		private final int width;
		private final int height;
		private final ActorRef view;
		
		public InitGridMsg(final int width, final int height, final ActorRef view) {
			this.width = width;
			this.height = height;
			this.view = view;
		}
		
		public int getWidth() {
			return this.width;
		}
		
		public int getHeight() {
			return this.height;
		}
		
		public ActorRef getView() {
			return this.view;
		}
	}
	
	/**
	 * This message allows to start the game.
	 */
	public static final class StartGameMsg { }
	
	/**
	 * This message allows to pause the game.
	 */
	public static final class PauseGameMsg { }
	
	/**
	 * This message contains the next state of a cell with a certain
	 * position, for the current generation that is being computed.
	 */
	public static final class CellNextStateMsg {
		private final Point position;
		private final boolean state;
		
		public CellNextStateMsg(final Point position, final boolean state) {
			this.position = position;
			this.state = state;
		}
		
		public Point getCellPosition() {
			return this.position;
		}
		
		public boolean getCellState() {
			return this.state;
		}
	}
	
	
	/**
	 * Creates Props for a grid actor.
	 * 
	 * @return a Props for creating a grid actor, which can then be further configured
	 */
	public static Props props() {
		return Props.create(GridActor.class);
	}
	
	/**
	 * Creates a grid actor.
	 */
	public GridActor() {
		this.timer = new Chrono();
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		this.initializingBehavior = receiveBuilder()
				.match(InitGridMsg.class, msg -> {
					initialize(msg.getHeight(), msg.getWidth(), msg.getView());
					// Changes state
					unstashAll();
					getContext().become(this.pausedBehavior);
				})
				.match(StartGameMsg.class, msg -> stash())
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.pausedBehavior = receiveBuilder()
				.match(StartGameMsg.class, msg -> {
					this.timer.start();
					// Distinguishes first start from resume
					if (this.notYetStarted) {
						this.calculatedGeneration.clear();
						this.nAliveCells = 0;
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell(new PrepareNextGenerationMsg(), ActorRef.noSender()));
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell(new ComputeMsg(getSelf()), ActorRef.noSender()));
					} else {
						unstashAll();
					}
					getContext().become(this.playingBehavior, false);
				})
				.match(CellNextStateMsg.class, msg -> stash())
				.match(InitGridMsg.class, resetMsg -> {
					this.cellsActorsMap.values().forEach(cellRef -> {
						getContext().stop(cellRef);
					});
					getContext().become(receiveBuilder()
							.match(Terminated.class, t -> this.cellsActorsMap.values().contains(t.actor()), t -> {
								this.nTerminatedCells++;
								if (this.nTerminatedCells == this.cellsActorsMap.size()) {
									initialize(resetMsg.getWidth(), resetMsg.getHeight(), resetMsg.getView());
									getContext().unbecome();
								}
							})
							.build(), false);
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(CellNextStateMsg.class, msg -> {
					if (msg.getCellState()) {
						this.nAliveCells++;
					}
					this.calculatedGeneration.put(msg.getCellPosition(), msg.getCellState());
					// If all the states of the current generation are computed...
					if (this.calculatedGeneration.size() == this.cellsActorsMap.size()) {
						// Updates generations number
						this.nGenerations++;
						// Calculates the averageTime
						this.timer.stop();
						final long elapsedTime = this.timer.getTime();
						this.averageTime += (elapsedTime - this.averageTime) / this.nGenerations;
						// Notify the actor view
						this.view.tell(new ViewActor.GenerationResultsMsg(
								this.nGenerations,
								new HashMap<Point, Boolean>(this.calculatedGeneration),
								elapsedTime,
								this.averageTime,
								this.nAliveCells), ActorRef.noSender());
						
						// Prepares and starts the computation for the new generation
						this.calculatedGeneration.clear();
						this.nAliveCells = 0;
						this.timer.start();
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell(new PrepareNextGenerationMsg(), ActorRef.noSender()));
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell(new ComputeMsg(getSelf()), ActorRef.noSender()));
					}
				})
				.match(PauseGameMsg.class, msg -> {
					this.timer.pause();
					getContext().unbecome();
				})
				.match(InitGridMsg.class, resetMsg -> {
					this.cellsActorsMap.values().forEach(cellRef -> {
						getContext().stop(cellRef);
					});
					getContext().become(receiveBuilder()
							.match(Terminated.class, t -> this.cellsActorsMap.values().contains(t.actor()), t -> {
								this.nTerminatedCells++;
								if (this.nTerminatedCells == this.cellsActorsMap.size()) {
									initialize(resetMsg.getWidth(), resetMsg.getHeight(), resetMsg.getView());
									getContext().unbecome();
								}
							})
							.build(), false);
				})
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
	}
	
	/*
	 * Calculates the references to the actors linked to the neighbors
	 * of the cell with the specified position.
	 */
	private Set<ActorRef> getCellNeighbours(final Point cellPosition) {
		final Set<ActorRef> neighbours = new HashSet<ActorRef>();
		for (int y = cellPosition.y - 1; y <= cellPosition.y + 1; y++) {
			for (int x = cellPosition.x - 1; x <= cellPosition.x + 1; x++) {
				if (cellPosition.y != y || cellPosition.x != x) {
					neighbours.add(this.cellsActorsMap.get(
							new Point((x + this.width) % this.width, (y + this.height) % this.height)));
				}
			}
		}
		return neighbours;
	}
	
	private void initialize(final int width, final int height, final ActorRef view) {
		// Initializes the fields
		this.width = width;
		this.height = height;
		this.view = view;
		this.cellsActorsMap = new HashMap<>();
		this.notYetStarted = true;
		this.nGenerations = 0;
		this.calculatedGeneration = new HashMap<>();
		this.nAliveCells = 0;
		this.averageTime = 0;
		
		// Creates cell actors and registers their references in a map
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				final ActorRef cellActor = getContext().actorOf(CellActor.props(x, y), "cell_" + x + "_" + y);
				this.cellsActorsMap.put(new Point(x, y), cellActor);
				getContext().watch(cellActor);
			}
		}
		// Sends neighbors to each cell
		this.cellsActorsMap.forEach((cellPos, cellRef) ->
			cellRef.tell(new CellActor.NeighboursMsg(getCellNeighbours(cellPos)), ActorRef.noSender()));
		// Initializes the cells with a random state
		this.cellsActorsMap.forEach((cellPos, cellRef) -> {
			boolean randomState = ThreadLocalRandom.current().nextBoolean();
			if (randomState) {
				this.nAliveCells++;
			}
			this.calculatedGeneration.put(cellPos, randomState);
			cellRef.tell(new CellActor.SetStateMsg(randomState), ActorRef.noSender());
		});
		
		// Notify the actor view with the initialized grid
		this.view.tell(new ViewActor.GenerationResultsMsg(
				this.nGenerations,
				new HashMap<Point, Boolean>(this.calculatedGeneration),
				0, 0,
				this.nAliveCells), ActorRef.noSender());
	}
	
	@Override
	public void preStart() {
		getContext().getSystem().scheduler().scheduleOnce(
				Duration.create(5, TimeUnit.SECONDS),
				getSelf(),
				new InitGridMsg(10, 10, null),
				getContext().system().dispatcher(),
				ActorRef.noSender());
	}
	
	@Override
	public Receive createReceive() {
		return this.initializingBehavior;
	}
	
}
