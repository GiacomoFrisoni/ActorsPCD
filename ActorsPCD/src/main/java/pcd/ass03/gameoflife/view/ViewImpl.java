package pcd.ass03.gameoflife.view;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.Map;
import java.util.Optional;

import akka.actor.ActorRef;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class ViewImpl extends BorderPane implements View {
	
	private static final int FRAME_SCALE = 70;
	private static final String WINDOW_TITLE = "\"The Game Of Life (Actors Version) - Giacomo Frisoni & Marcin Pabich\"";
	
	private Stage stage;
	private ActorRef gridActor, viewActor;
	
	@FXML private CellMapViewer cellMapViewer;
	@FXML private MenuPanel menuPanel;
	
	public ViewImpl(final Stage stage) {
		//Try to load FXML resource
		Optional<String> result = ViewUtils.loadFXML(this, "View.fxml");
		
		if (!result.isPresent()) {
			//Get the reference for the stage
			this.stage = stage;	
			
			//Settings for the stage
			this.stage.setTitle(WINDOW_TITLE);
			this.stage.setResizable(false);
			
			//Set the main scene, dimensions and icons
			this.setScene();
			this.setViewDimensions();
			this.setIcons();
			
			//Set references for CellMapViewer and MenuPanel
			this.cellMapViewer.setMenuRef(this.menuPanel);
			this.menuPanel.setCellMapRef(this.cellMapViewer);
		} else {
			MessageUtils.showFXMLException(this.getClass().getSimpleName(), result.get());
		}
	}

	@Override
	public void show() {
		this.stage.show();
		this.initInternalComponentSize();	
	}

	@Override
	public void close() {
		this.stage.close();
	}

	@Override
	public void setGridActor(final ActorRef gridActor) {
		this.gridActor = gridActor;
		this.menuPanel.setGridActorRef(this.gridActor);
	}

	@Override
	public void setViewActor(final ActorRef viewActor) {
		this.viewActor = viewActor;
		this.menuPanel.setViewActorRef(this.viewActor);
	}

	@Override
	public void drawCells(final Map<Point, Boolean> cells) {
		this.cellMapViewer.drawCells(cells);
	}

	@Override
	public void setStarted() {
		this.menuPanel.setStarted();
	}

	@Override
	public void setStopped() {
		this.menuPanel.setStopped();
	}

	@Override
	public void reset() {
		this.menuPanel.reset();
		this.cellMapViewer.reset();
	}
	
	/**
	 * Set the scene for the view
	 */
	private void setScene() {
		final Scene scene = new Scene(this);
		this.stage.setScene(scene);
		
		this.stage.setOnCloseRequest(e -> {
			this.close();
	        Platform.exit();
	        System.exit(0);
		});
	}
	
	/**
	 * Initialize the dimensions of the view
	 */
	private void setViewDimensions() {
		final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    	this.setWidth((gd.getDisplayMode().getWidth() * FRAME_SCALE) / 100);
    	this.setHeight((gd.getDisplayMode().getHeight() * FRAME_SCALE) / 100);
    	this.setMinWidth((gd.getDisplayMode().getWidth() * FRAME_SCALE) / 100);
    	this.setMinHeight((gd.getDisplayMode().getHeight() * FRAME_SCALE) / 100);
	}
	
	/**
	 * Set the icons for the view
	 */
	private void setIcons() {
		this.stage.getIcons().addAll(
				new Image(("file:res/icon_gol_16x16.png")),
				new Image(("file:res/icon_gol_32x32.png")),
				new Image(("file:res/icon_gol_64x64.png")));
	}
	
	private void initInternalComponentSize() {
		this.cellMapViewer.init();
		this.menuPanel.init();
	}

}
