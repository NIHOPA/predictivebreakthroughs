package gov.nih.opa.mcl;

import gov.nih.opa.pico.PICOCommonCmd;
import gov.nih.opa.pico.ShowStackArgs;
import picocli.CommandLine;

@CommandLine.Command(name = "mcl", subcommands = { MCLRun.class }, mixinStandardHelpOptions = true, scope = CommandLine.ScopeType.INHERIT)
public class MCL {

	@CommandLine.Mixin
	private ShowStackArgs showStackArgs;

	public static void main(String[] args) {
		PICOCommonCmd.runCommandLine(new MCL(), args);
	}

}
