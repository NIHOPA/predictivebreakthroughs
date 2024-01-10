package gov.nih.opa.mcl;

public interface MCLParameters {

	public float getInflation();

	public float getLoop();

	public float getPruningThreshold();

	public int getMaxIterations();

	public float getShiftThreshold();

	public boolean isRegularized();

	public String getInput();

	public String getOutput();

}
