package au.gov.amsa.animator;

public class Model {

	volatile long timeStep = 0;

	public void updateModel(long timeStep) {
		this.timeStep = timeStep;
	}
}
