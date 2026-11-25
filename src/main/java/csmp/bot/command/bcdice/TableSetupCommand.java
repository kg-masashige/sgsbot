package csmp.bot.command.bcdice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.BcDiceApiService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.modals.Modal;

/**
 * オリジナル表のコマンド.
 * 登録はモーダルで入力し、/roll に登録したコマンドを指定すると表を振る。
 * @author kgmas
 *
 */
public class TableSetupCommand implements ISlashCommand {

	private static final String SUB_ADD = "登録";
	private static final String SUB_LIST = "一覧";
	private static final String SUB_CLEAR = "クリア";

	/**
	 * モーダルID. 「コマンド名:」で始める。
	 */
	public static final String MODAL_ID = "sgstable:add";

	private static final String FIELD_COMMAND = "command";
	private static final String FIELD_DICE = "dice";
	private static final String FIELD_TITLE = "title";
	private static final String FIELD_RESULTS = "results";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String subcommand = event.getSubcommandName();
		if (SUB_ADD.equals(subcommand)) {
			event.replyModal(createModal()).queue();
		} else if (SUB_LIST.equals(subcommand)) {
			list(event);
		} else if (SUB_CLEAR.equals(subcommand)) {
			BcDiceApiService.getInstance().setTableInfo(event.getGuild().getIdLong(), null);
			event.reply("ダイスボット表をクリアしました。").queue();
		}
	}

	/**
	 * 登録用のモーダルを作成する.
	 * @return モーダル
	 */
	public static Modal createModal() {
		TextInput command = TextInput.create(FIELD_COMMAND, TextInputStyle.SHORT)
				.setPlaceholder("例: 感情表")
				.setRequiredRange(1, 100)
				.build();
		TextInput dice = TextInput.create(FIELD_DICE, TextInputStyle.SHORT)
				.setPlaceholder("例: 1D6")
				.setRequiredRange(1, 50)
				.build();
		TextInput title = TextInput.create(FIELD_TITLE, TextInputStyle.SHORT)
				.setPlaceholder("例: シノビガミ感情表")
				.setRequiredRange(1, 100)
				.build();
		TextInput results = TextInput.create(FIELD_RESULTS, TextInputStyle.PARAGRAPH)
				.setPlaceholder("1:共感\n2:友情\n3:愛情")
				.setRequiredRange(1, 4000)
				.build();

		return Modal.create(MODAL_ID, "オリジナル表の登録")
				.addComponents(
						Label.of("反応させるコマンド", "/roll にこのコマンドを指定すると表を振ります。", command),
						Label.of("ダイス", "BCDiceのコマンドで指定してください。", dice),
						Label.of("表の名前", title),
						Label.of("結果一覧", "1行に「ダイスの結果:出力したい文章」の形で入力してください。", results))
				.build();
	}

	@Override
	public void executeModal(ModalInteractionEvent event) {
		if (!MODAL_ID.equals(event.getModalId())) {
			return;
		}
		String command = event.getValue(FIELD_COMMAND).getAsString().trim();
		String dice = event.getValue(FIELD_DICE).getAsString().trim();
		String title = event.getValue(FIELD_TITLE).getAsString().trim();
		Map<String, String> resultMap = parseResults(event.getValue(FIELD_RESULTS).getAsString());

		if (command.isEmpty() || dice.isEmpty() || title.isEmpty()) {
			event.reply("コマンド、ダイス、表の名前を入力してください。").setEphemeral(true).queue();
			return;
		}
		if (resultMap.isEmpty()) {
			event.reply("結果一覧は1行に「ダイスの結果:出力したい文章」の形で入力してください。").setEphemeral(true).queue();
			return;
		}

		BcDiceApiService.getInstance().putTable(event.getGuild().getIdLong(), command, dice, title, resultMap);
		event.reply("コマンド：" + command + "に" + title + "を登録しました。").queue();
	}

	/**
	 * 結果一覧を解析する.
	 * 1行に「ダイスの結果:出力したい文章」の形で書く。区切りは半角・全角のコロンのどちらでもよい。
	 * 区切りがない行と、ダイスの結果が空の行は無視する。
	 * @param text 入力された結果一覧
	 * @return ダイスの結果 → 出力したい文章
	 */
	public static Map<String, String> parseResults(String text) {
		Map<String, String> resultMap = new LinkedHashMap<>();
		if (text == null) {
			return resultMap;
		}
		for (String line : text.split("\\R", -1)) {
			int sepIndex = indexOfSeparator(line);
			if (sepIndex < 0) {
				continue;
			}
			String value = line.substring(0, sepIndex).trim();
			String explain = line.substring(sepIndex + 1).trim();
			if (value.isEmpty()) {
				continue;
			}
			resultMap.put(value, explain);
		}
		return resultMap;
	}

	private static int indexOfSeparator(String line) {
		int half = line.indexOf(':');
		int full = line.indexOf('：');
		if (half < 0) {
			return full;
		}
		if (full < 0) {
			return half;
		}
		return Math.min(half, full);
	}

	@SuppressWarnings("unchecked")
	private void list(SlashCommandInteractionEvent event) {
		Map<String, Map<String, Object>> tableInfo = BcDiceApiService.getInstance()
				.getTableInfo(event.getGuild().getIdLong());
		if (tableInfo == null || tableInfo.isEmpty()) {
			event.reply("オリジナル表は登録されていません。").setEphemeral(true).queue();
			return;
		}
		List<String> lines = new ArrayList<>();
		lines.add("■オリジナル表（/roll <コマンド> で振れます）");
		for (Map.Entry<String, Map<String, Object>> entry : tableInfo.entrySet()) {
			Map<String, Object> detail = entry.getValue();
			Map<String, String> result = (Map<String, String>) detail.get("result");
			lines.add(entry.getKey() + "：" + detail.get("title") + "（" + detail.get("dice") + "、"
					+ result.size() + "件）");
		}
		DiscordUtil.reply(event, String.join("\n", lines), true);
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData("/sgstable 登録",
				"入力画面でコマンド・ダイス・表の名前・結果一覧を登録する。/roll <コマンド> で表を振る。"));
		list.add(new CommandHelpData("/sgstable 一覧", "登録したオリジナル表を表示する。"));
		list.add(new CommandHelpData("/sgstable クリア", "登録したオリジナル表をすべて削除する。"));
		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "ダイスの結果を表に当てはめるオリジナル表を管理します。")
				.setContexts(InteractionContextType.GUILD)
				.addSubcommands(
						new SubcommandData(SUB_ADD, "オリジナル表を登録します。同じコマンドの表は上書きします。"),
						new SubcommandData(SUB_LIST, "登録したオリジナル表を表示します。"),
						new SubcommandData(SUB_CLEAR, "登録したオリジナル表をすべて削除します。"));
	}

	@Override
	public String getCommandName() {
		return "sgstable";
	}

}
