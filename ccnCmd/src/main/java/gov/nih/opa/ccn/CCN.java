package gov.nih.opa.ccn;

import picocli.CommandLine;

@CommandLine.Command(name = "ccn", subcommands = { CCNFirstOrder.class,
		CCNPMIDs.class, }, mixinStandardHelpOptions = true, scope = CommandLine.ScopeType.INHERIT)
public class CCN {

	@CommandLine.Mixin
	private ShowStackArgs showStackArgs;

	public static void main(String[] args) {
		CCNCommonCmd.runCommandLine(new CCN(), args);
	}

}
