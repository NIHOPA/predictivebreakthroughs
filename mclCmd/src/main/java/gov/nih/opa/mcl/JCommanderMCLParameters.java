package gov.nih.opa.mcl;

import com.beust.jcommander.Parameter;

public class JCommanderMCLParameters implements MCLParameters {

	@Parameter(names = { "--inflation", "-I" }, description = "Inflation", order = 3)
	private float inflation = 2.0f;

	@Parameter(names = "--loop", description = "Self Loop", order = 4)
	private float loop = 0.001f;

	@Parameter(names = "--prune", description = "Minimum Probability", order = 5)
	private float pruningThreshold = 0.0005f;

	@Parameter(names = "--max", description = "Maximum Iterations", order = 6)
	private int maxIterations = 250;

	@Parameter(names = "--shift", description = "Shift Threshold", order = 7)
	private float shiftThreshold = 0.0001f;

	@Parameter(names = "--regularized", description = "Use R-MCL", order = 4)
	private boolean regularized = true;

	@Parameter(names = "--in", description = "Input Spreadsheet (CSV, TSV, or Excel) with header and three columns with any name (node a, node b, weight)", required = true, order = 1)
	private String input;

	@Parameter(names = "--out", description = "Output", required = true, order = 2)
	private String output;

	public JCommanderMCLParameters() {

	}

	public float getInflation() {
		return inflation;
	}

	public JCommanderMCLParameters setInflation(float inflation) {
		this.inflation = inflation;
		return this;
	}

	public float getLoop() {
		return loop;
	}

	public JCommanderMCLParameters setLoop(float loop) {
		this.loop = loop;
		return this;
	}

	public float getPruningThreshold() {
		return pruningThreshold;
	}

	public JCommanderMCLParameters setPruningThreshold(float pruningThreshold) {
		this.pruningThreshold = pruningThreshold;
		return this;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public JCommanderMCLParameters setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public float getShiftThreshold() {
		return shiftThreshold;
	}

	public JCommanderMCLParameters setShiftThreshold(float shiftThreshold) {
		this.shiftThreshold = shiftThreshold;
		return this;
	}

	public boolean isRegularized() {
		return regularized;
	}

	public JCommanderMCLParameters setRegularized(boolean regularized) {
		this.regularized = regularized;
		return this;
	}

	public String getInput() {
		return input;
	}

	public JCommanderMCLParameters setInput(String input) {
		this.input = input;
		return this;
	}

	public String getOutput() {
		return output;
	}

	public JCommanderMCLParameters setOutput(String output) {
		this.output = output;
		return this;
	}

	@Override
	public String toString() {
		return "MCLParameters{" + "inflation=" + inflation + ", loop=" + loop + ", pruningThreshold=" + pruningThreshold + ", maxIterations=" + maxIterations
				+ ", shiftThreshold=" + shiftThreshold + ", regularized=" + regularized + ", input='" + input + '\'' + ", output='" + output + '\''
				+ ", input='" + input + '\'' + '}';
	}
}
