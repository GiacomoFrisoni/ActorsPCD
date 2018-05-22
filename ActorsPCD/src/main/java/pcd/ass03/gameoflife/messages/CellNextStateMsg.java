package pcd.ass03.gameoflife.messages;

import java.awt.Point;

public final class CellNextStateMsg {
	
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
