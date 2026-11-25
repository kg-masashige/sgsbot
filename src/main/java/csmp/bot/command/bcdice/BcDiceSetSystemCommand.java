package csmp.bot.command.bcdice;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.BcDiceApiService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * ダイスボットシステム設定コマンド.
 * @author kgmas
 *
 */
public class BcDiceSetSystemCommand implements ISlashCommand {

	private static final String OPTION_SYSTEM = "システム名";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String systemName = event.getOption(OPTION_SYSTEM).getAsString().trim();
		BcDiceApiService bcDiceApi = BcDiceApiService.getInstance();
		String systemId = bcDiceApi.resolveSystemId(systemName);
		if (systemId == null) {
			event.reply(systemName + "が見つかりません。/bcdicesearchで正しいシステム名を検索して設定してください。")
					.setEphemeral(true).queue();
			return;
		}

		event.reply(systemName + "を設定します。").setEphemeral(true).queue();
		bcDiceApi.putGuildSystem(event.getGuild().getIdLong(), systemId);
		event.getChannel().sendMessage("ダイスボットを" + bcDiceApi.getSystemNames().get(systemId) + "に設定しました。")
				.queue();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdiceset <指定したいシステム名>",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）を設定する。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "BCDiceで判定する際のシステム名を設定します。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_SYSTEM,
						"BCDiceのシステム名（ソード・ワールド2.5、シノビガミなど）を指定してください。", true);
	}

	@Override
	public String getCommandName() {
		return "bcdiceset";
	}

}
