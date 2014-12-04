package au.gov.amsa.navigation;

public class Vector {
	private final double x;
	private final double y;

	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public Vector minus(Vector v) {
		return new Vector(x - v.x, y - v.y);
	}

	public Vector add(Vector v) {
		return new Vector(x + v.x, y + v.y);
	}

	public double dot(Vector v) {
		return x * v.x + y * v.y;
	}

}
