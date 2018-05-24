package pcd.ass03.gameoflife.messages;

import java.awt.Point;
import java.util.Map;

public final class GenerationResultsMsg {
	
	private final Map<Point, Boolean> generationComputed;
	private final long timeElapsed;
	private final int nCellsAlive;
	
	public GenerationResultsMsg(final Map<Point, Boolean> generationComputed, final long timeElapsed, final int nCellsAlive) {
		this.generationComputed = generationComputed;
		this.timeElapsed = timeElapsed;
		this.nCellsAlive = nCellsAlive;
	}
	
	public Map<Point, Boolean> getGenerationComputed() {
		return this.generationComputed;
	}
	
	public long getTimeElapsed() {
		return this.timeElapsed;
	}
	
	public int getNumberOfCellsAlive() {
		return this.nCellsAlive;
	}
	
}
