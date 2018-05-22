package pcd.ass03.gameoflife.actors;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.gameoflife.messages.CellNextStateMsg;
import pcd.ass03.gameoflife.messages.NeighboursMsg;
import pcd.ass03.gameoflife.messages.SetStateMsg;

/**
 * This actor represents a grid for the Conway's Game Of Life.
 *
 */
public class GridActor extends AbstractActorWithStash {
	
	private final int width;
	private final int height;
	private final Map<Point, ActorRef> cellsActorsMap;
	
	private boolean notYetStarted;
	private final Map<Point, Boolean> calculatedGeneration;
	
	private final LoggingAdapter log;
	private Receive pausedBehavior;
	private Receive playingBehavior;
	
	
	/**
	 * Create Props for a grid actor.
	 * 
	 * @param width
	 * 		the width of the grid to be passed to the actor's constructor.
	 * @param height
	 * 		the height of the grid to be passed to the actor's constructor.
	 * @return a Props for creating grid actor, which can then be further configured
	 */
	public static Props props(final int width, final int height) {
		return Props.create(GridActor.class, width, height);
	}
	
	/**
	 * Creates a new grid actor.
	 * 
	 * @param width
	 * 		the width of the grid
	 * @param height
	 * 		the height of the grid
	 */
	public GridActor(final int width, final int height) {
		this.width = width;
		this.height = height;
		this.cellsActorsMap = new HashMap<>();
		
		this.notYetStarted = true;
		this.calculatedGeneration = new HashMap<>();
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		this.pausedBehavior = receiveBuilder()
				.matchEquals("play", msg -> {
					if (this.notYetStarted) {
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell("prepareNextGeneration", ActorRef.noSender()));
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell("compute", getSelf()));
					} else {
						unstashAll();
					}
					getContext().become(this.playingBehavior, false);
				})
				.match(CellNextStateMsg.class, msg -> stash())
				.matchAny(msg -> log.info("Received unknown message: " + msg))
				.build();
		
		this.playingBehavior = receiveBuilder()
				.match(CellNextStateMsg.class, msg -> {
					this.calculatedGeneration.put(msg.getCellPosition(), msg.getCellState());
					// If all the states of the current generation are computed...
					if (this.calculatedGeneration.size() == this.cellsActorsMap.size()) {
						// Notify the actor view
						for (int y = 0; y < this.height; y++) {
							for (int x = 0; x < this.width; x++) {
								System.out.print(this.calculatedGeneration.get(new Point(x, y)) ? "O " : "X ");
							}
							System.out.println();
						}
						System.out.println();
						// Prepares and starts the computation for the new generation
						this.calculatedGeneration.clear();
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell("prepareNextGeneration", ActorRef.noSender()));
						this.cellsActorsMap.values().forEach(cellRef -> cellRef.tell("compute", getSelf()));
					}
				})
				.matchEquals("pause", msg -> getContext().unbecome())
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
	
	@Override
	public void preStart() {
		// Creates cell actors and registers their references in a map
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				this.cellsActorsMap.put(new Point(x, y), getContext().actorOf(CellActor.props(x, y), "cell_" + x + "_" + y));
			}
		}
		// Sends neighbors to each cell
		this.cellsActorsMap.forEach((cellPos, cellRef) ->
			cellRef.tell(new NeighboursMsg(getCellNeighbours(cellPos)), ActorRef.noSender()));
		// Initializes as alive at most the half of the cells
		/*
		for (int i = 0; i < this.cellsActorsMap.size()/2; i++) {
			final int randomCellX = ThreadLocalRandom.current().nextInt(this.width);
			final int randomCellY = ThreadLocalRandom.current().nextInt(this.height);
			this.cellsActorsMap.get(new Point(randomCellX, randomCellY)).tell(new SetStateMsg(true), ActorRef.noSender());
		}*/
		this.cellsActorsMap.values().forEach(cellRef ->
			cellRef.tell(new SetStateMsg(ThreadLocalRandom.current().nextBoolean()), ActorRef.noSender()));
	}
	
	@Override
	public Receive createReceive() {
		return this.pausedBehavior;
	}
	
}
