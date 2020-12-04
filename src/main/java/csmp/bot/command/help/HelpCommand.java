package csmp.bot.command.help;

import java.util.List;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

/**
 * ヘルプコマンド.
 * @author kgmas
 *
 */
public class HelpCommand implements IDiscordCommand {

	private String message = "";

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if ((dmd.getText().startsWith("/sche") || dmd.getText().startsWith("/sgs"))
				&& dmd.getText().contains("help")) {
			return true;
		}
		if (dmd.getText().equals("/デイコードヘルプ")) {
			return true;
		}

		return false;
	}

	public void setCommandList(List<IDiscordCommand> commandList) {
		message = "■コマンド一覧（<>は除いて発言してください）\r\n";
		message += createCommandHelp(getCommandHelpData());
		for (IDiscordCommand command : commandList) {
			CommandHelpData helpData = command.getCommandHelpData();
			message += createCommandHelp(helpData);
		}
	}

	private String createCommandHelp(CommandHelpData helpData) {
		if (helpData == null) {
			return "";
		}

		if (helpData.getHelpList() != null) {
			String helps = "";
			for (CommandHelpData data : helpData.getHelpList()) {
				helps += createCommandHelp(data);
			}
			return helps;
		}

		String help = helpData.getCommandText() + "\r\n" +
				"　　　" + helpData.getExplainMessage() + "\r\n";
		if (helpData.getExplainDetail() != null) {
			help += "　　　" + helpData.getExplainDetail() + "\r\n";
		}
		help += "\r\n";
		return help;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage(message);
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/sgshelp もしくは /デイコードヘルプ", "コマンド一覧の表示。");
	}

}
