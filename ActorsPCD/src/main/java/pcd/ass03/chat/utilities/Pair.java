package pcd.ass03.chat.utilities;

public class Pair<X, Y> {

	private final X first;
	private final Y second;
	
	public Pair(final X first, final Y second) {
		this.first = first;
		this.second = second;
	}
	
	public X getFirst() {
		return this.first;
	}
	
	public Y getSecond() {
		return this.second;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Pair
				&& this.first.equals(((Pair<?, ?>)obj).getFirst())
				&& this.second.equals(((Pair<?, ?>)obj).getSecond());
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + "]";
	}
	
}
