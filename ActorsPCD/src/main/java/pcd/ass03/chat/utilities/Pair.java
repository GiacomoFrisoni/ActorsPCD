package pcd.ass03.chat.utilities;

import java.io.Serializable;
import java.util.Objects;

public class Pair<X, Y> implements Serializable {

	private static final long serialVersionUID = -6655306989714494647L;
	private final X first;
	private final Y second;
	
	public Pair(final X first, final Y second) {
		Objects.requireNonNull(first);
		Objects.requireNonNull(second);
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
	public boolean equals(final Object obj) {
		return obj instanceof Pair
				&& this.first.equals(((Pair<?, ?>)obj).first)
				&& this.second.equals(((Pair<?, ?>)obj).second);
	}

	@Override
	public String toString() {
		return "Pair [first=" + first + ", second=" + second + "]";
	}
	
}
