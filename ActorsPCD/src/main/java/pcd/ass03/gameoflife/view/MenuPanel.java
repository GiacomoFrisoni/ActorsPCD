package pcd.ass03.gameoflife.view;

import java.awt.Dimension;
import java.util.Optional;

import akka.actor.ActorRef;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import pcd.ass03.gameoflife.actors.GridActor;
import pcd.ass03.gameoflife.actors.ViewActor;

public class MenuPanel extends VBox {
	
	private static final double DEFAULT_REFRESH_RATE = 1000;
	
	@FXML private TextField mapWidth, mapHeight;
	@FXML private MiniMap miniMap;
	@FXML private Pane miniMapContainer;
	@FXML private Label currentPosition, viewableCells, generation, elapsedTime, aliveCells, errorLabel, sliderValue, avgElapsedTime;
	@FXML private Button start, stop, reset;
	@FXML private VBox loadingStatus;
	@FXML private ProgressBar progress;
	@FXML private Slider slider;
	
	private ActorRef gridActor, viewActor;
	private CellMapViewer cellMapViewer;
	
	private boolean isStarted = false;
	
	public MenuPanel() {
		Optional<String> result = ViewUtils.loadFXML(this, "MenuPanel.fxml");

		if (!result.isPresent()) {
			this.errorLabel.setVisible(false);
			this.slider.setValue(DEFAULT_REFRESH_RATE);
			this.sliderValue.setText("" + (int) DEFAULT_REFRESH_RATE);
			this.setActionListeners();
			this.setPropertiesListeners();	
		} else {
			MessageUtils.showFXMLException(this.getClass().getSimpleName(), result.get());			
		}
	}
	
	/**
	 * Initialize the component after viewing to set correctly the size
	 */
	public void init() {
		this.miniMap.setWidth(this.miniMapContainer.getWidth());
		this.miniMap.setHeight(this.miniMapContainer.getHeight());	
	}
	
	/**
	 * Set the reference to the grid actor
	 * @param gridActor
	 * 		Reference to the grid actor
	 */
	public void setGridActorRef(final ActorRef gridActor) {
		this.gridActor = gridActor;
	}
	
	/**
	 * Set the reference to the view actor
	 * @param viewActor
	 * 		Reference to view actor
	 */
	public void setViewActorRef(final ActorRef viewActor) {
		this.viewActor = viewActor;
	}
	
	/**
	 * Set the reference to cell map viewer
	 * @param cellMapViewer
	 * 		Reference to cell map viewer
	 */
	public void setCellMapRef(final CellMapViewer cellMapViewer) {
		this.cellMapViewer = cellMapViewer;
	}
	
	/**
	 * Reset the menu panel
	 */
	public void reset() {
		
	}
	
	/**
	 * Set as stopped the state of menu panel
	 */
	public void setStopped() {
		
	}
	
	/**
	 * Set as started the state of menu panel
	 */
	public void setStarted() {
		
	}
	
	/**
	 * Set the limits of the minimap, when the map dimension is known
	 * @param x
	 * 		X limit of the minimap (width)
	 * @param y
	 * 		Y limit of the minimap (height)
	 */
	public void setMiniMapLimits(final int x, final int y) {
		this.miniMap.setLimits(x, y);
	}
	
	/**
	 * Updates the minimap, changing the preview position
	 * @param xPos
	 * 		X position of the preview
	 * @param yPos
	 * 		Y position of the preview
	 */
	public void updateMiniMap(final int xPos, final int yPos) {
		this.miniMap.drawCurrentPosition(xPos, yPos);
		Platform.runLater(() -> {
			this.currentPosition.setText(xPos + ", " + yPos);
		});	
	}
	
	/**
	 * Updates the viewable cells values, when they're known
	 * @param drawableXCells
	 * 		Cells drawable in width on the cell map viewer
	 * @param drawableYCells
	 * 		Cells drawable in height on the cell map viewer
	 */
	public void updatePreviewValues(final int drawableXCells, final int drawableYCells) {
		Platform.runLater(() ->{
			this.viewableCells.setText("W: " + drawableXCells + ", H: " + drawableYCells);
		});
	}
	
	/**
	 * Set the buttons and slider action listeners
	 */
	private void setActionListeners() {
		this.start.setOnMouseClicked(e -> {
			if (!isStarted) {
				getReadyToStart();
			} else {
				this.start.setDisable(true);
				this.stop.setDisable(false);
				this.reset.setDisable(true);
				
				gridActor.tell(new GridActor.StartGameMsg(), ActorRef.noSender());
				viewActor.tell(new ViewActor.StartVisualizationMsg(), ActorRef.noSender());
			}
		});
		
		this.stop.setOnMouseClicked(e -> {
			this.gridActor.tell(new GridActor.PauseGameMsg(), ActorRef.noSender());
			this.viewActor.tell(new ViewActor.StopVisualizationMsg(), ActorRef.noSender());
			
			//Enable the button
			Platform.runLater(() -> {
				this.start.setDisable(false);
				this.stop.setDisable(true);
				this.reset.setDisable(false);
			});
		});
		
		this.reset.setOnMouseClicked(e -> {		
			
			//Reset started status
			isStarted = false;
			this.cellMapViewer.reset();
			this.miniMap.reset();
			this.gridActor.tell(new GridActor.ResetGameMsg(), ActorRef.noSender());
			this.viewActor.tell(new ViewActor.ResetVisualizationMsg(), ActorRef.noSender());
			
			//Reset buttons
			Platform.runLater(() -> {
				this.reset.setDisable(true);
				this.start.setDisable(false);
				this.stop.setDisable(true);
				
				this.mapWidth.setDisable(false);
				this.mapHeight.setDisable(false);
			});
		});
		
		this.slider.setOnMouseReleased(e -> {
			this.viewActor.tell(new ViewActor.ChangeRefreshRateMsg((long)this.slider.getValue()), ActorRef.noSender());
		});
	}

	/**
	 * Set the bindings for values
	 */
	private void setPropertiesListeners() {		
		this.generation.textProperty().bind(ViewDataManager.getInstance().getGeneration().asString());
		this.elapsedTime.textProperty().bind(ViewDataManager.getInstance().getElapsedTime().asString("%d ms"));
		this.aliveCells.textProperty().bind(ViewDataManager.getInstance().getAliveCells().asString());
		this.avgElapsedTime.textProperty().bind(ViewDataManager.getInstance().getAvgElapsedTime().asString("%d ms"));
		
		this.slider.valueProperty().addListener(listener -> {	
			final int subdivision = (int) (this.slider.getValue() / 100);
			this.sliderValue.setText("" + (subdivision * 100));
		});
	}
	
	/**
	 * Execute checks and start, if can
	 */
	private void getReadyToStart() {
		//Get the dimension
		final Optional<Dimension> dimension = this.getMapDimension();
		
		//If it's valid and present
		if (dimension.isPresent()) {
			//Set the map dimensions
			this.cellMapViewer.setDimension(dimension.get());
			
			//Sending message, you can start now!
			gridActor.tell(new GridActor.InitGridMsg(dimension.get().width, dimension.get().height, viewActor), ActorRef.noSender());
			viewActor.tell(new ViewActor.StartVisualizationMsg(), ActorRef.noSender());
			gridActor.tell(new GridActor.StartGameMsg(), ActorRef.noSender());
			this.isStarted = true;
			
			//Getting started
			Platform.runLater(() -> {
				this.start.setDisable(true);
				this.stop.setDisable(false);
				this.reset.setDisable(true);
				
				this.mapWidth.setDisable(true);
				this.mapHeight.setDisable(true);
			});

		}
	}
	
	/**
	 * Read from the menu the desired width and height of the map
	 * @return
	 * 		Optional.empty() if something went wrong <br/>
	 * 		Optional.of(new Dimension...) if values are ok
	 */
	private Optional<Dimension> getMapDimension() {
		this.errorLabel.setVisible(false);
		final String width = this.mapWidth.getText();
		final String height = this.mapHeight.getText();
		
		if (width != null && height != null) {
			if (!width.isEmpty() && !height.isEmpty()) {
				if (width.chars().allMatch(Character::isDigit) && height.chars().allMatch(Character::isDigit)) {
					if (Integer.parseInt(width) > 0 && Integer.parseInt(height) > 0) {
						return Optional.of(new Dimension(Integer.parseInt(width), Integer.parseInt(height)));
					}
				}
			} 
		}
			
		this.errorLabel.setVisible(true);
		return Optional.empty();	
	}
}
