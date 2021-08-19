package csmp.bot.command.bcdice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

/**
 * 参加者全員のプロット（botに対するダイレクトメッセージ）を表示するコマンド.
 * @author kgmas
 *
 */
public class PlotSetCommand implements IDiscordCommand,IDiscordSlashCommand {

	private static Map<Long, Long> userMessageIdMap = new ConcurrentHashMap<>();

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/plot", "プロットを行う。設定した値は公開されるまで自分しか見えない。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("プロットの処理を行います。")
				.addOption(SlashCommandOption.create(SlashCommandOptionType.STRING, "プロット値", "プロット値を指定してください。設定した値は/openplotで公開されるまであなたにしか見えません。",true))
				;

	}

	@Override
	public String getCommandName() {
		return "plot";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String message = dmd.getInteraction().getFirstOptionStringValue().orElse(null);
		PlotOpenCommand.setPlot(dmd.getChannel().getId(), dmd.getUser().getId(), message);
		dmd.getInteraction().createImmediateResponder().setFlags(MessageFlag.EPHEMERAL).setContent(message + "をプロットしました。").respond();

		User user = dmd.getUser();
		dmd.getChannel().sendMessage(user.getNickname(dmd.getGuild()).orElse(user.getName()) + "がプロットしました。");
	}

}
