package csmp.bot.command.character;

import java.util.Map;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.entities.channel.attribute.IWebhookContainer;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * キャラクターシートとチャンネルを紐付けるコマンド.
 */
public class LinkCharacterSheetCommand implements ISlashCommand {

	private static final String OPTION_URL = "キャラクターシートurl";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String sheetUrl = event.getOption(OPTION_URL).getAsString().trim();
		if (!(event.getChannel() instanceof IWebhookContainer channel)) {
			event.reply("このチャンネルでは実行できません。テキストチャンネルで実行してください。").setEphemeral(true).queue();
			return;
		}

		event.deferReply(true).queue();
		String webhookUrl = DiscordUtil.getWebhookUrl(event.getHook(), channel);
		if (webhookUrl == null) {
			return;
		}

		Map<String, Object> result = CsmpService.getInstance().registerCharacterSheet(webhookUrl, sheetUrl);
		if (result != null && "ok".equals(result.get("result"))) {
			DiscordUtil.sendMessage(event.getHook(), "キャラクターシートを登録しました。キャラクターシートでDRボタンを押してください。");
		} else {
			DiscordUtil.sendMessage(event.getHook(), "キャラクターシートの登録に失敗しました。再度コマンドを実行してください。");
		}
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/link <キャラクターシートURL>",
				"キャラクターシート上でDRボタンを押した時に結果をこのチャンネルに反映させる。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(),
				"キャラクターシート上のDRボタンを押した時に結果をこのチャンネルに反映させることができます。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_URL,
						"キャラクターシート倉庫に登録しているキャラクターシートのURLを指定してください。", true);
	}

	@Override
	public String getCommandName() {
		return "link";
	}

}
