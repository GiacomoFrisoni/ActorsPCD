package pcd.ass03.gameoflife.view;

import java.awt.Dimension;
import java.util.Optional;

import akka.actor.ActorRef;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MenuPanel extends VBox {

	@FXML
	private TextField mapWidth, mapHeight;
	
	@FXML
	private MiniMap miniMap;
	
	@FXML
	private Label currentPosition, viewableCells, generation, elapsedTime, aliveCells, errorLabel, loadingLabel, sliderValue, avgElapsedTime;
	
	@FXML
	private Button start, stop, reset;
	
	@FXML
	private Pane cellMapContainer;
	
	@FXML
	private VBox loadingStatus;
	
	@FXML
	private ProgressBar progress;
	
	@FXML
	private Slider slider;
	
	private ActorRef gridActor, viewActor;
	private CellMapViewer cellMapViewer;
	
	public MenuPanel() {
		Optional<String> result = ViewUtils.loadFXML(this, "MenuPanel.fxml");
		
		if (!result.isPresent()) {
			this.errorLabel.setVisible(false);
			this.setActionListeners();
			this.setPropertiesListeners();
		} else {
			MessageUtils.showFXMLException(this.getClass().getSimpleName(), result.get());
		}
	}
	
	
	public void setGridActorRef(final ActorRef gridActor) {
		this.gridActor = gridActor;
	}
	
	public void setViewActorRef(final ActorRef viewActor) {
		this.viewActor = viewActor;
	}
	
	public void setCellMapRef(final CellMapViewer cellMapViewer) {
		this.cellMapViewer = cellMapViewer;
	}
	
	public void init() {
		//this.miniMap.setLimits(x, y);
	}
	
	public void reset() {
		
	}
	
	public void setStopped() {
		
	}
	
	public void setStarted() {
		
	}
	
	public void updateMiniMap(final int xPos, final int yPos) {
		
	}
	
	private void setActionListeners() {
		start.setOnMouseClicked(e -> {
			getReadyToStart();
		});
		
		stop.setOnMouseClicked(e -> {
			this.stop.setDisable(true);
			this.gridActor.tell("stop", ActorRef.noSender());
		});
		
		reset.setOnMouseClicked(e -> {
			this.reset.setDisable(true);
			this.gridActor.tell("reset", ActorRef.noSender());
		});
	}
	
	private void setPropertiesListeners() {
		this.generation.textProperty().bind(ViewDataManager.getInstance().getGeneration().asString());
		this.elapsedTime.textProperty().bind(ViewDataManager.getInstance().getElapsedTime().asString());
		this.aliveCells.textProperty().bind(ViewDataManager.getInstance().getAliveCells().asString());
		this.avgElapsedTime.textProperty().bind(ViewDataManager.getInstance().getAvgElapsedTime().asString());
		
		this.loadingLabel.textProperty().bind(ViewDataManager.getInstance().getMessage());
	}
	
	private void getReadyToStart() {
		//Get the dimension
		final Optional<Dimension> dimension = this.getMapDimension();
		
		//If it's valid and present
		if (dimension.isPresent()) {
			//Set the map dimensions
			this.cellMapViewer.setDimension(dimension.get());
			
			//Getting started
			this.start.setDisable(true);
			this.stop.setDisable(false);
			
			//Sending message, you can start now!
			this.gridActor.tell("play", ActorRef.noSender());
		}
	}
	
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
