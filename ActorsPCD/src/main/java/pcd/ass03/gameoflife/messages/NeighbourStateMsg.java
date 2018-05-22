package pcd.ass03.gameoflife.messages;

public final class NeighbourStateMsg {

	private final boolean neighbourState;
	
	public NeighbourStateMsg(final boolean neighbourState) {
		this.neighbourState = neighbourState;
	}
	
	public boolean getNeighbourState() {
		return this.neighbourState;
	}
	
}
