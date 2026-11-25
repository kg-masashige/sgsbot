package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * シナリオ秘密送付コマンド.
 * @author kgmas
 *
 */
public class ScenarioSendSecretCommand implements ISlashCommand {

	private static final String OPTION_SECRET = "秘密名";
	private static final String OPTION_CHANNEL = "送付先チャンネル";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		Map<Object, Object> secretMap = CsmpService.getInstance().getGuildScenarioInfo().get(event.getGuild().getIdLong());
		if (secretMap == null) {
			event.reply("シナリオ情報が設定されていません。").setEphemeral(true).queue();
			return;
		}

		String secretName = event.getOption(OPTION_SECRET).getAsString().trim();
		if (!(event.getOption(OPTION_CHANNEL).getAsChannel() instanceof GuildMessageChannel channel)) {
			event.reply("チャンネルが正しく指定できていません。").setEphemeral(true).queue();
			return;
		}

		String textMessage = findSecretMessage(secretMap, secretName);
		if (textMessage == null) {
			event.reply(secretName + "の秘密がシナリオ情報内に見つかりません。").setEphemeral(true).queue();
			return;
		}

		DiscordUtil.sendToChannel(channel, textMessage);
		event.reply(channel.getName() + "のチャンネルに" + secretName + "の秘密を送りました。").queue();
	}

	/**
	 * 秘密名に一致する秘密のメッセージを作成する.
	 * PCで始まる場合はPCの秘密、それ以外（先頭のNPCは省略可）はNPCの秘密を探す。
	 * @param secretMap シナリオ情報
	 * @param secretName 秘密名
	 * @return メッセージ. 見つからない場合はnull.
	 */
	@SuppressWarnings("unchecked")
	public static String findSecretMessage(Map<Object, Object> secretMap, String secretName) {
		if (secretName.toUpperCase().startsWith("PC")) {
			List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
			for (Map<String, Object> map : pcList) {
				String name = "PC" + CsmpService.text(map, "name");
				if (secretName.equalsIgnoreCase(name)) {
					return "■" + name + "の秘密：\r\n" + CsmpService.text(map, "secret");
				}
			}
		}

		String secretNameEdit = "";
		if (secretName.toUpperCase().startsWith("NPC")) {
			secretNameEdit = secretName.substring(3);
		}
		List<Map<String, Object>> npcList = (List<Map<String, Object>>)secretMap.get("npc");
		if (npcList != null) {
			for (Map<String, Object> map : npcList) {
				String name = CsmpService.text(map, "name");
				if (secretName.equalsIgnoreCase(name)
						|| secretNameEdit.equalsIgnoreCase(name)) {
					return "■" + name + "の秘密：\r\n" + CsmpService.text(map, "secret");
				}
			}
		}
		return null;
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgssend <秘密名> <送付先チャンネル>",
				"送付先チャンネルに指定された秘密を張り付ける。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "シナリオシートの秘密を特定のチャンネルに対して送付します。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_SECRET,
						"シナリオシート上の秘密名を指定してください。PCの秘密の場合は先頭にPCとつけてください。", true)
				.addOption(OptionType.CHANNEL, OPTION_CHANNEL,
						"秘密を送付するチャンネルを選択してください。", true);
	}

	@Override
	public String getCommandName() {
		return "sgssend";
	}

}
