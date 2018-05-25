package pcd.ass03.gameoflife.view;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Map;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public class CellMapViewer extends BorderPane {
	
	private static final int CELL_SIZE = 9;
	private static final int CELL_OFFSET = CELL_SIZE + 1;
	private static final Color BACKGROUND_COLOR = new Color(0.23, 0.23, 0.23, 1);
	private static final Color ALIVE_CELL_COLOR = Color.AQUA;
	
	private int xPos, yPos;
	private int maxXMapNeeded, maxYMapNeeded;
	private int actualXMap, actualYMap;
	private int drawableXCells, drawableYCells;
	
	private Map<Point, Boolean> cells;
	
	@FXML Canvas cellMap;
	@FXML Button left, top, right, bottom;
	
	private MenuPanel menuPanel;
	
	public CellMapViewer() {
		Optional<String> result = ViewUtils.loadFXML(this, "CellMapViewer.fxml");
		
		if (!result.isPresent()) {
			this.xPos = 0;
			this.yPos = 0;
			this.maxXMapNeeded = 0;
			this.maxYMapNeeded = 0;		
		} else {
			MessageUtils.showFXMLException(getClass().getSimpleName(), result.get());
		}
	}
	
	
	public void init() {
		this.setCanvasDimension();
		this.setActionListeners();
	}
	
	
	public void drawCells(final Map<Point, Boolean> cells) {	
		this.cells = cells;
		this.draw();
	}
	
	public void setMenuRef(MenuPanel menuPanel) {
		this.menuPanel = menuPanel;
	}
	
	/**
	 * Calculate the map limit dimension when get the official width and height 
	 * @param mapDimension
	 * 		Dimension of the map
	 */
	public void setDimension(final Dimension mapDimension) {
		//How many maps I need to draw all the cells
		this.actualXMap = mapDimension.width;
		this.actualYMap = mapDimension.height;
		
		this.maxXMapNeeded = (int) (this.actualXMap / this.drawableXCells);
		this.maxYMapNeeded = (int) (this.actualYMap / this.drawableYCells);
	}
	
	public void reset() {
		
	}
	
	
	private void setCanvasDimension() {
		final double centralPanelWidth = this.getWidth() - this.left.getWidth() - this.right.getWidth();
		final double centralPanelHeight = this.getHeight() - this.top.getHeight() - this.bottom.getHeight();
		
		this.cellMap.setWidth(centralPanelWidth);
		this.cellMap.setHeight(centralPanelHeight);
		
		this.drawableXCells = (int) (cellMap.getWidth() / CELL_OFFSET);
		this.drawableYCells = (int) (cellMap.getHeight() / CELL_OFFSET);
	}
	
	private void draw() {
		if (cells != null) {
			//TODO elimiate this
			setDimension(new Dimension(5, 5));
			
			final int xStartPos = xPos * drawableXCells;
			final int yStartPos = yPos * drawableYCells;		
			
			final int xStopPos = Math.min(drawableXCells, actualXMap);
			final int yStopPos = Math.min(drawableYCells, actualYMap);
			
			final GraphicsContext gc = cellMap.getGraphicsContext2D();
			gc.clearRect(0, 0, cellMap.getWidth(), cellMap.getHeight());
			gc.setFill(ALIVE_CELL_COLOR);	
			
			for (int y = 0; y < yStopPos; y++) {
				for (int x = 0; x < xStopPos; x++) {
					//Get the point in right place (i + offset) (j + offset)
					final Boolean value = cells.get(new Point(x + (xStartPos), y + (yStartPos)));
					
					//Check if it's null and it's alive
					if (value != null) {
						if (value) {
	        				gc.fillRect(x * CELL_OFFSET, y * CELL_OFFSET, CELL_SIZE, CELL_SIZE);		   
						}	
					} else {
						System.out.println("[" + x + ", " + y + "] - [" + (x+xStartPos) + ", " + (y+yStartPos) + "] is not present in the set");
					}
				}
			}
		}
	}
	
	
	private void setActionListeners() {
		this.left.setOnMouseClicked(e -> {
			if (this.xPos > 0) {
				this.xPos -= 1;
				updateState();
			}
		});
		
		this.top.setOnMouseClicked(e -> {
			if (this.yPos > 0) {
				this.yPos -= 1;
				updateState();
			}
		});
		
		this.right.setOnMouseClicked(e -> {
			if (this.xPos < this.maxXMapNeeded) {
				this.xPos += 1;
				updateState();
			}
		});
		
		this.bottom.setOnMouseClicked(e -> {
			if (this.yPos < this.maxYMapNeeded) {
				this.yPos += 1;
				updateState();
			}
		});
	}
	
	
	private void updateState() {
		draw();
		this.menuPanel.updateMiniMap(xPos, yPos);
	}
}
