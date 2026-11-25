package csmp.bot.command.bcdice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
 * ダイスボットシステム名検索コマンド.
 * @author kgmas
 *
 */
public class BcDiceSearchSystemNameCommand implements ISlashCommand {

	private static final String OPTION_FRAGMENT = "システム名の一部";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String input = event.getOption(OPTION_FRAGMENT).getAsString().trim();
		if (input.isEmpty()) {
			event.reply("検索したい文字列が設定されていません。").setEphemeral(true).queue();
			return;
		}

		List<String> resultList = search(BcDiceApiService.getInstance().getSystemNames(), input);
		if (resultList.isEmpty()) {
			event.reply("指定された名前で見つかりませんでした。" + input).setEphemeral(true).queue();
		} else {
			String message = "■システム名（検索結果）：\n" + String.join("\n", resultList);
			DiscordUtil.reply(event, message, true);
		}
	}

	/**
	 * システム名を部分一致で検索する. IDと表示名をNFKC正規化・小文字化して比較する.
	 * @param systemNames システムID → 表示名
	 * @param input 検索文字列
	 * @return 一致したシステムの表示名
	 */
	public static List<String> search(Map<String, String> systemNames, String input) {
		String fragment = normalize(input);
		List<String> resultList = new ArrayList<>();
		for (Entry<String, String> entry : systemNames.entrySet()) {
			if (normalize(entry.getKey()).contains(fragment) ||
					normalize(entry.getValue()).contains(fragment)) {
				resultList.add(entry.getValue());
			}
		}
		return resultList;
	}

	private static String normalize(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdicesearch <システム名の一部（ビガミ、shinobi）など>",
				"ダイスボットで指定できるシステム名を検索する。（部分一致検索）");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "bcdicesetで指定するためのシステム名のフルネームを検索します。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_FRAGMENT,
						"BCDiceのシステム名（ソード、シノビなど）を指定してください。（部分一致検索）", true);
	}

	@Override
	public String getCommandName() {
		return "bcdicesearch";
	}

}
