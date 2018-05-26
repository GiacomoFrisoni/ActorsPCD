package pcd.ass03.gameoflife.view;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class ViewDataManager {
	
	private final static String IDLE_MESSAGE = "Idle";
	
	private static volatile ViewDataManager singleton;
	
	private final SimpleLongProperty generation = new SimpleLongProperty();
	private final SimpleLongProperty aliveCells = new SimpleLongProperty();
	private final SimpleLongProperty elapsedTime = new SimpleLongProperty();
	private final SimpleLongProperty avgElapsedTime = new SimpleLongProperty();
	
	private ViewDataManager() {
		this.generation.set(0);
		this.aliveCells.set(0);
		this.elapsedTime.set(0);
		this.avgElapsedTime.set(0);
	}
	
	/**
	 * This method returns the DataManager.
	 * If the DataManager is null it creates a new one on the first call (thread-safe).
	 * @return the data manager.
	 */
	public static ViewDataManager getInstance() {
		if (singleton == null ) {
			synchronized (ViewDataManager.class) {
				if (singleton == null) {
					singleton = new ViewDataManager();
				}
			}
		}
		
		return singleton;
	}
	
	
	/**
	 * Get the property of current generation number, ready to bind
	 * @return
	 * 		Property representing the current generation number
	 */
	public SimpleLongProperty getGeneration() {
		return generation;
	}

	/**
	 * Get the property of current alive cells, ready to bind
	 * @return
	 * 		Property representing the current alive cells
	 */
	public SimpleLongProperty getAliveCells() {
		return aliveCells;
	}

	/**
	 * Get the property of last elapsed time, ready to bind
	 * @return
	 * 		Property represeting last elapsed time
	 */
	public SimpleLongProperty getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Get the property of average elapsed time, ready to bind
	 * @return
	 * 		Property represeting average elapsed time
	 */
	public SimpleLongProperty getAvgElapsedTime() {
		return avgElapsedTime;
	}

	/**
	 * Set the current generation number
	 * @param generation
	 * 		generation number to set
	 */
	public void setGeneration(final long generation) {
		Platform.runLater(() -> {
			this.generation.set(generation);
		});
	}
	
	/**
	 * Set the number of alive cells
	 * @param aliveCells
	 * 		number of alive cells to set
	 */
	public void setAliveCells(final long aliveCells) {
		Platform.runLater(() -> {
			this.aliveCells.set(aliveCells);
		});
	}
	
	/**
	 * Set the elapsed time
	 * @param elapsedTime
	 * 		elapsed time to set
	 */
	public void setElapsedTime(final long elapsedTime) {
		Platform.runLater(() -> {
			this.elapsedTime.set(elapsedTime);
		});
	}
	
	/**
	 * Set the average elapsed time
	 * @param avgElapsedTime
	 * 		average elapsed time to set
	 */
	public void setAvgElapsedTime(final long avgElapsedTime) {
		Platform.runLater(() -> {
			this.avgElapsedTime.set(avgElapsedTime);
		});
	}
}
