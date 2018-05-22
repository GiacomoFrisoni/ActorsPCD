package pcd.ass03.gameoflife.messages;

public final class SetStateMsg {
	
	private final boolean state;
	
	public SetStateMsg(final boolean state) {
		this.state = state;
	}
	
	public boolean getState() {
		return this.state;
	}
	
}
