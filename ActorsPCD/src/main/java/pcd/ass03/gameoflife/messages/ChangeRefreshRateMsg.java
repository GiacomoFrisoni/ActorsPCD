package pcd.ass03.gameoflife.messages;

public final class ChangeRefreshRateMsg {
	
	private final long refreshRate;
	
	public ChangeRefreshRateMsg(final long refreshRate) {
		this.refreshRate = refreshRate;
	}
	
	public long getRefreshRate() {
		return this.refreshRate;
	}
	
}
