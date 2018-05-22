package pcd.ass03.gameoflife.messages;

public final class CellEvaluationResultMsg {
	
	private final boolean toCompute;
	
	public CellEvaluationResultMsg(final boolean toCompute) {
		this.toCompute = toCompute;
	}
	
	public boolean isToCompute() {
		return this.toCompute;
	}
	
}
