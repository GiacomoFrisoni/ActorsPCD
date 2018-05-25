package pcd.ass03.gameoflife.utilities;

/**
 * This class handles a custom timer.
 */
public class Chrono {

	private boolean running;
	private boolean paused;
	private long startTime;

	/**
	 * Constructs a new timer.
	 */
	public Chrono() {
		this.running = false;
		this.paused = false;
		this.startTime = System.currentTimeMillis();
	}
	
	/**
	 * Starts the timer.
	 */
	public void start() {
		this.running = true;
		if (this.paused) {
			this.startTime -= System.currentTimeMillis();
			this.paused = false;
		} else {
			this.startTime = System.currentTimeMillis();
		}
	}
	
	/**
	 * Stops the timer.
	 */
	public void stop() {
		this.startTime = getTime();
		this.running = false;
	}
	
	/**
	 * Pauses the timer.
	 */
	public void pause() {
		this.running = false;
		this.paused = true;
		this.startTime = getTime();
	}

	/**
	 * @return the time elapsed between start and stop in milliseconds.
	 */
	public long getTime() {
		if (this.running) {
			return System.currentTimeMillis() - startTime;
		} else {
			return startTime;
		}
	}
	
}
