package pcd.ass03.gameoflife.view;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class MiniMap extends Canvas {

	private static final Color BACKGROUND_COLOR = new Color(0.23, 0.23, 0.23, 1);
	private int cellSizeX, cellSizeY;
	
	
	public MiniMap() { }
	
	
	/**
	 * Set the limits of the small map preview
	 * @param x
	 * 		max x reachable (in width)
	 * @param y
	 * 		max y reachable (in height)
	 */
	public void setLimits(final int x, final int y) {
		this.cellSizeX = (int) (this.getWidth() / (x+1));
		this.cellSizeY = (int) (this.getHeight() / (y+1));
		drawCurrentPosition(0, 0);
	}

	/**
	 * Draw a rectangle corresponding at current position on the map
	 * @param x
	 * 		x coordinate of the rectangle
	 * @param y
	 * 		y coordinate of the rectangle
	 */
	public void drawCurrentPosition(final int x, final int y) {	
		final GraphicsContext gc = getGraphicsContext2D();
		gc.setFill(Color.ORANGE);
		
		Platform.runLater(() -> {	
			gc.clearRect(0, 0, getWidth(), getHeight());				
			gc.fillRect((x * cellSizeX), (y * cellSizeY), cellSizeX , cellSizeY);
		});	
	}
	
	/**
	 * Clear the small map preview
	 */
	public void reset() {
		final GraphicsContext gc = getGraphicsContext2D();
		gc.setFill(BACKGROUND_COLOR);
		
		Platform.runLater(() -> {
			gc.clearRect(0, 0, getWidth(), getHeight());
		});
	}
}
