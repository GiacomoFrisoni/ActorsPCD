package pcd.ass03.gameoflife.actors;

import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * This actor represents a cell for the Conway's Game Of Life.
 * With the adopted solution, the generation completion requires N + 1 messages for each cell.
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
	 * This message allows to define the neighbors of the cell.
	 */
	public static final class NeighboursMsg {
		private final Set<ActorRef> neighbours;
		
		public NeighboursMsg(final Set<ActorRef> neighbours) {
			this.neighbours = new HashSet<>(neighbours);
		}
		
		public Set<ActorRef> getNeighbours() {
			return this.neighbours;
		}
	}
	
	/**
	 * This message allows to set the cell state during the initialization.
	 */
	public static final class SetStateMsg {
		private final boolean state;
		
		public SetStateMsg(final boolean state) {
			this.state = state;
		}
		
		public boolean getState() {
			return this.state;
		}
	}
	
	/**
	 * This message allows to prepare a new generation.
	 *
	 */
	public static final class PrepareNextGenerationMsg { }
	
	/**
	 * This message requests the cell computation for the current generation.
	 */
	public static final class ComputeMsg {
		private final ActorRef sender;
		
		public ComputeMsg(final ActorRef sender) {
			this.sender = sender;
		}
		
		public ActorRef getSender() {
			return this.sender;
		}
	}
	
	/**
	 * This message represents the state of a neighbor cell.
	 */
	public static final class NeighbourStateMsg {
		private final boolean neighbourState;
		
		public NeighbourStateMsg(final boolean neighbourState) {
			this.neighbourState = neighbourState;
		}
		
		public boolean getNeighbourState() {
			return this.neighbourState;
		}
	}

	/**
	 * This message represents the state informations of a neighbor cell after its computation.
	 */
	public static final class NeighbourNextStateMsg {
		private final boolean nextState;
		private final boolean isChanged;
		
		public NeighbourNextStateMsg(final boolean nextState, final boolean isChanged) {
			this.nextState = nextState;
			this.isChanged = isChanged;
		}
		
		public boolean getNeighbourNextState() {
			return this.nextState;
		}
		
		public boolean isNeighbourStateChanged() {
			return this.isChanged;
		}
	}

	
	/**
	 * Creates Props for a cell actor.
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
	 * Creates a cell actor.
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
					// Saves the neighbors
					this.neighbours.clear();
					this.neighbours.addAll(neighboursMsg.getNeighbours());
					// Enters in a state in which it knows its neighbors...
					getContext().become(receiveBuilder()
							// With the neighbors knowledge, the cell can notify its initialization state...
							.match(SetStateMsg.class, stateMsg -> {
								this.nextState = stateMsg.getState();
								this.neighbours.forEach(n -> n.tell(new NeighbourStateMsg(this.nextState), ActorRef.noSender()));
								// If the state is configured and all the neighbors have been notified, initialization is complete...
								if (this.arrivedNeighboursStates == N_NEIGHBOURS) {
									unstashAll();
									getContext().become(this.activeBehavior);
								} else {
									// ... Otherwise changes the behavior: once its state has been set, it can no longer be changed directly
									getContext().become(receiveBuilder()
											.match(NeighbourStateMsg.class, neighbourMsg -> {
												this.arrivedNeighboursStates++;
												if (neighbourMsg.getNeighbourState()) {
													this.nextAliveNeighbours++;
												}
												// Checks if the initialization is completed
												if (this.arrivedNeighboursStates == N_NEIGHBOURS) {
													unstashAll();
													getContext().become(this.activeBehavior);
												}
											})
											.match(PrepareNextGenerationMsg.class, msg -> stash())
											.match(ComputeMsg.class, msg -> stash())
											.match(NeighbourNextStateMsg.class, msg -> stash())
											.matchAny(msg -> this.log.info("Received unknown message: " + msg))
											.build());
								}
							})
							// ... Or it can be notified by a neighbor
							.match(NeighbourStateMsg.class, neighbourMsg -> {
								this.arrivedNeighboursStates++;
								if (neighbourMsg.getNeighbourState()) {
									this.nextAliveNeighbours++;
								}
							})
							.match(PrepareNextGenerationMsg.class, msg -> stash())
							.match(ComputeMsg.class, msg -> stash())
							.match(NeighbourNextStateMsg.class, msg -> stash())
							.matchAny(msg -> this.log.info("Received unknown message: " + msg))
							.build());
				})
				.match(NeighbourNextStateMsg.class, msg -> stash())
				.matchAny(msg -> this.log.info("0 Received unknown message: " + msg))
				.build();
		
		this.activeBehavior = receiveBuilder()
				.match(PrepareNextGenerationMsg.class, nextMsg -> {
					// Resets data
					this.arrivedNeighboursStates = 0;
					this.stateChanged = false;
					this.nArrivedNextStateNeighbours = 0;
					// Swaps
					this.state = this.nextState;
					this.aliveNeighbours = this.nextAliveNeighbours;
					unstashAll();
					getContext().become(receiveBuilder()
							.match(ComputeMsg.class, computeMsg -> {
								this.stateChanged = false;
								// Considers the next state as unchanged by default
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
								computeMsg.getSender().tell(new GridActor.CellNextStateMsg(new Point(this.x, this.y), this.nextState), ActorRef.noSender());
								
								// Sends the computed state to the neighbors (even if unchanged)
								this.neighbours.forEach(n -> n.tell(new NeighbourNextStateMsg(this.nextState, this.stateChanged), ActorRef.noSender()));
								
								// The computation of a cell is completed if its next state and that of the neighbors have been determined
								if (this.nArrivedNextStateNeighbours == N_NEIGHBOURS) {
									unstashAll();
									getContext().become(this.activeBehavior);
								} else {
									// Waits for the completion of the neighboring cells computation
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
											.match(PrepareNextGenerationMsg.class, msg -> stash())
											.match(ComputeMsg.class, msg -> stash())
											.matchAny(msg -> this.log.info("1 Received unknown message: " + msg))
											.build());
								}
							})
							.match(NeighbourNextStateMsg.class, stateMsg -> {
								this.nArrivedNextStateNeighbours++;
								if (stateMsg.isNeighbourStateChanged()) {
									this.nextAliveNeighbours += stateMsg.getNeighbourNextState() ? 1 : -1;
								}
							})
							.match(PrepareNextGenerationMsg.class, msg -> stash())
							.matchAny(msg -> this.log.info("2 Received unknown message: " + msg))
							.build());
				})
				.match(ComputeMsg.class, msg -> stash())
				.match(NeighbourNextStateMsg.class, msg -> stash())
				.matchAny(msg -> this.log.info("3 Received unknown message: " + msg))
				.build();
	}
	
	@Override
	public Receive createReceive() {
		return this.initializingBehavior;
	}

}
