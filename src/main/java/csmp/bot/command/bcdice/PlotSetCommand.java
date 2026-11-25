package csmp.bot.command.bcdice;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * プロット値を設定するコマンド.
 * 設定した値は /openplot で公開されるまで本人にしか見えない。
 * @author kgmas
 *
 */
public class PlotSetCommand implements ISlashCommand {

	private static final String OPTION_PLOT = "プロット値";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String value = event.getOption(OPTION_PLOT).getAsString();
		String displayName = DiscordUtil.getDisplayName(event);
		PlotOpenCommand.setPlot(event.getChannelIdLong(), event.getUser().getIdLong(), displayName, value);

		event.reply(value + "をプロットしました。").setEphemeral(true).queue();
		event.getChannel().sendMessage(displayName + "がプロットしました。").queue();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/plot <プロット値>", "プロットを行う。設定した値は公開されるまで自分しか見えない。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "プロットの処理を行います。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_PLOT,
						"プロット値を指定してください。設定した値は/openplotで公開されるまであなたにしか見えません。", true);
	}

	@Override
	public String getCommandName() {
		return "plot";
	}

}
