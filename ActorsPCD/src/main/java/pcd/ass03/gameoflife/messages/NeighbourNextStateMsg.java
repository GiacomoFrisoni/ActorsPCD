package pcd.ass03.gameoflife.messages;

public final class NeighbourNextStateMsg {

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
