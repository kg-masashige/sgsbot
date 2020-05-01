package csmp.bot.model;

import java.util.List;

public class CommandHelpData {

	private String commandText;

	private String explainMessage;

	private String explainDetail;

	private List<CommandHelpData> helpList;

	public CommandHelpData(String commandText, String explainMessage) {
		this.commandText = commandText;
		this.explainMessage = explainMessage;
	}

	public CommandHelpData(String commandText, String explainMessage, String explainDetail) {
		this.commandText = commandText;
		this.explainMessage = explainMessage;
		this.explainDetail = explainDetail;
	}

	public CommandHelpData(List<CommandHelpData> helpList) {
		this.helpList = helpList;
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

	public List<CommandHelpData> getHelpList() {
		return helpList;
	}

}
