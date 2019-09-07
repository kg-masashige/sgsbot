package csmp.bot.model;

public class CommandHelpData {

	private String commandText;

	private String explainMessage;

	private String explainDetail;

	public CommandHelpData(String commandText, String explainMessage) {
		this.commandText = commandText;
		this.explainMessage = explainMessage;
	}

	public CommandHelpData(String commandText, String explainMessage, String explainDetail) {
		this.commandText = commandText;
		this.explainMessage = explainMessage;
		this.explainDetail = explainDetail;
	}

	public String getCommandText() {
		return commandText;
	}

	public String getExplainMessage() {
		return explainMessage;
	}

	public String getExplainDetail() {
		return explainDetail;
	}

}
