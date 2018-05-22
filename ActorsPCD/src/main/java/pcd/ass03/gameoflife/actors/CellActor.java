package pcd.ass03.gameoflife.actors;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import pcd.ass03.gameoflife.messages.CellNextStateMsg;
import pcd.ass03.gameoflife.messages.NeighbourNextStateMsg;
import pcd.ass03.gameoflife.messages.NeighbourStateMsg;
import pcd.ass03.gameoflife.messages.NeighboursMsg;
import pcd.ass03.gameoflife.messages.SetStateMsg;

/**
 * This actor represents a cell for the Conway's Game Of Life.
 *
 */
public class CellActor extends AbstractActorWithStash {
	
	private static final int N_NEIGHBOURS = 8;
	
	private final int x;
	private final int y;
	private final Set<ActorRef> neighbours;
	private boolean state;
	private boolean nextState;
	private int aliveNeighbours;
	private int nextAliveNeighbours;
	
	private int arrivedNeighboursStates;
	private boolean stateChanged;
	private int nArrivedNextStateNeighbours;

	private final LoggingAdapter log;
	private Receive initializingBehavior;
	private Receive activeBehavior;
	
	
	/**
	 * Create Props for a cell actor.
	 * 
	 * @param x
	 * 		the x coordinate of the cell to be passed to the actor's constructor.
	 * @param y
	 * 		the y coordinate of the cell to be passed to the actor's constructor.
	 * @return a Props for creating cell actor, which can then be further configured
	 */
	public static Props props(final int x, final int y) {
		return Props.create(CellActor.class, x, y);
	}
	
	/**
	 * Creates a new cell actor.
	 * 
	 * @param x
	 * 		the x coordinate of the cell
	 * @param y
	 * 		the y coordinate of the cell
	 */
	public CellActor(final int x, final int y) {
		this.x = x;
		this.y = y;
		this.neighbours = new HashSet<>();
		this.state = false;
		this.nextState = false;
		this.aliveNeighbours = 0;
		this.nextAliveNeighbours = 0;
		
		this.arrivedNeighboursStates = 0;
		this.stateChanged = false;
		this.nArrivedNextStateNeighbours = 0;
		
		this.log = Logging.getLogger(getContext().getSystem(), this);
		
		this.initializingBehavior = receiveBuilder()
				.match(NeighboursMsg.class, neighboursMsg -> {
					this.neighbours.clear();
					this.neighbours.addAll(neighboursMsg.getNeighbours());
					// The cell knows its neighbors...
					getContext().become(receiveBuilder()
							// Now it can notify its state or it can be notified by a neighbor
							.match(SetStateMsg.class, stateMsg -> {
								this.nextState = stateMsg.getState();
								this.neighbours.forEach(n -> n.tell(new NeighbourStateMsg(this.nextState), getSelf()));
								// If the state is configured and all the neighbors have been notified, initialization is complete...
								if (this.arrivedNeighboursStates == N_NEIGHBOURS) {
									unstashAll();
									getContext().become(this.activeBehavior);
								} else {
									// ... Otherwise changes the behavior: once its state has been set, it can no longer be changed
									getContext().become(receiveBuilder()
											.match(NeighbourStateMsg.class, neighbourMsg -> {
												this.arrivedNeighboursStates++;
												if (neighbourMsg.getNeighbourState()) {
													this.nextAliveNeighbours++;
												}
												if (this.arrivedNeighboursStates == N_NEIGHBOURS) {
													unstashAll();
													getContext().become(this.activeBehavior);
												}
											})
											.matchEquals("prepareNextGeneration", msg -> stash())
											.matchEquals("compute", msg -> stash())
											.matchAny(msg -> this.log.info("Received unknown message: " + msg))
											.build());
								}
							})
							.match(NeighbourStateMsg.class, neighbourMsg -> {
								this.arrivedNeighboursStates++;
								if (neighbourMsg.getNeighbourState()) {
									this.nextAliveNeighbours++;
								}
							})
							.matchEquals("prepareNextGeneration", msg -> stash())
							.matchEquals("compute", msg -> stash())
							.matchAny(msg -> this.log.info("Received unknown message: " + msg))
							.build());
				})
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
		
		this.activeBehavior = receiveBuilder()
				.matchEquals("prepareNextGeneration", nextMsg -> {
					// Resets
					this.arrivedNeighboursStates = 0;
					this.stateChanged = false;
					this.nArrivedNextStateNeighbours = 0;
					// Swaps
					this.state = this.nextState;
					this.aliveNeighbours = this.nextAliveNeighbours;
					unstashAll();
					getContext().become(receiveBuilder()
							.matchEquals("compute", computeMsg -> {
								this.stateChanged = false;
								this.nextState = this.state;
								// Evaluates an update only if necessary
								if (this.state || (!this.state && this.aliveNeighbours > 0)) {
									if (this.state) {
										if (this.aliveNeighbours < 2 || this.aliveNeighbours > 3) {
											this.nextState = false;
											this.stateChanged = true;
										}
									} else {
										if (this.aliveNeighbours == 3) {
											this.nextState = true;
											this.stateChanged = true;
										}
									}
								}
								
								// Sends the result for the current generation
								getSender().tell(new CellNextStateMsg(new Point(this.x, this.y), this.nextState), ActorRef.noSender());
								
								this.neighbours.forEach(n -> n.tell(new NeighbourNextStateMsg(this.nextState, this.stateChanged), ActorRef.noSender()));
								
								if (this.nArrivedNextStateNeighbours == N_NEIGHBOURS) {
									unstashAll();
									getContext().become(this.activeBehavior);
								} else {
									getContext().become(receiveBuilder()
											.match(NeighbourNextStateMsg.class, stateMsg -> {
												this.nArrivedNextStateNeighbours++;
												if (stateMsg.isNeighbourStateChanged()) {
													this.nextAliveNeighbours += stateMsg.getNeighbourNextState() ? 1 : -1;
												}
												if (this.nArrivedNextStateNeighbours == N_NEIGHBOURS) {
													unstashAll();
													getContext().become(this.activeBehavior);
												}
											})
											.matchEquals("prepareNextGeneration", msg -> stash())
											.matchEquals("compute", msg -> stash())
											.matchAny(msg -> this.log.info("Received unknown message: " + msg))
											.build());
								}
							})
							.match(NeighbourNextStateMsg.class, stateMsg -> {
								this.nArrivedNextStateNeighbours++;
								if (stateMsg.isNeighbourStateChanged()) {
									this.nextAliveNeighbours += stateMsg.getNeighbourNextState() ? 1 : -1;
								}
							})
							.matchEquals("prepareNextGeneration", msg -> stash())
							.matchAny(msg -> this.log.info("Received unknown message: " + msg))
							.build());
				})
				.matchEquals("compute", msg -> stash())
				.match(NeighbourNextStateMsg.class, msg -> stash())
				.matchAny(msg -> this.log.info("Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public Receive createReceive() {
		return this.initializingBehavior;
	}

}
