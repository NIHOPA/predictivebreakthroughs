package gov.nih.opa.ccn;

import gov.nih.opa.pico.PICOCommonCmd;
import gov.nih.opa.pico.ShowStackArgs;
import picocli.CommandLine;

@CommandLine.Command(name = "ccn", subcommands = { CCNFirstOrder.class,
		CCNPMIDs.class, }, mixinStandardHelpOptions = true, scope = CommandLine.ScopeType.INHERIT)
public class CCN {

	@CommandLine.Mixin
	private ShowStackArgs showStackArgs;

	public static void main(String[] args) {
		PICOCommonCmd.runCommandLine(new CCN(), args);
	}

}
