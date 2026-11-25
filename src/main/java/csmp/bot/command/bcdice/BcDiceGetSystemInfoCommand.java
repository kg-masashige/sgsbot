package csmp.bot.command.bcdice;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.BcDiceApiService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * ダイスボットシステム説明取得コマンド.
 * @author kgmas
 *
 */
public class BcDiceGetSystemInfoCommand implements ISlashCommand {

	private static final String OPTION_SYSTEM = "システム名";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String systemName = event.getOption(OPTION_SYSTEM).getAsString().trim();
		BcDiceApiService bcDiceApi = BcDiceApiService.getInstance();
		String systemId = bcDiceApi.resolveSystemId(systemName);
		if (systemId == null) {
			event.reply("指定されたシステムがダイスボットに存在しません。：" + systemName).setEphemeral(true).queue();
			return;
		}

		// システム情報の取得に時間がかかる場合があるため、先に応答を保留する。
		event.deferReply(true).queue();
		String systemInfo = bcDiceApi.getSystemInfoMessage(systemId);
		DiscordUtil.sendMessage(event.getHook(),
				"システム：" + bcDiceApi.getSystemNames().get(systemId) + "(" + systemId + ")\n" + systemInfo);
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdiceget <確認したいシステム名>",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）のコマンド一覧を表示する。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "指定したシステム名のコマンド一覧を自分だけに表示します。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_SYSTEM,
						"BCDiceのシステム名（ソード・ワールド2.5、シノビガミなど）を指定してください。", true);
	}

	@Override
	public String getCommandName() {
		return "bcdiceget";
	}

}
