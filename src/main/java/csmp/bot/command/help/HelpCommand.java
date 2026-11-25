package csmp.bot.command.help;

import java.util.List;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * ヘルプコマンド.
 * @author kgmas
 *
 */
public class HelpCommand implements ISlashCommand {

	private final String message;

	public HelpCommand(List<ISlashCommand> commandList) {
		StringBuilder sb = new StringBuilder(
				"■コマンド一覧（<>は除いて実行してください。/を押してコマンド一覧が出てこない場合、botを承認し直す必要があります。）\r\n");
		sb.append(createCommandHelp(getCommandHelpData()));
		for (ISlashCommand command : commandList) {
			sb.append(createCommandHelp(command.getCommandHelpData()));
		}
		message = sb.toString();
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

	/**
	 * @return ヘルプのメッセージ
	 */
	public String getMessage() {
		return message;
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		DiscordUtil.reply(event, message, true);
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/sgshelp", "コマンド一覧の表示。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "ヘルプを表示します。")
				.setContexts(InteractionContextType.GUILD);
	}

	@Override
	public String getCommandName() {
		return "sgshelp";
	}

}
