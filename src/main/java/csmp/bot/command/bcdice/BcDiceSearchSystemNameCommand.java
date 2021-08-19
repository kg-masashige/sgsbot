package csmp.bot.command.bcdice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;
import csmp.utl.DiscordUtil;

/**
 * ダイスボットシステム説明取得コマンド.
 * @author kgmas
 *
 */
public class BcDiceSearchSystemNameCommand implements IDiscordCommand,IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/search systemname ")) {
			return true;
		}

		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length < 3) {
			return false;
		}
		if (dmd.getCommandArray()[2].length() == 0) {
			return false;
		}

		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String fragment = Normalizer.normalize(dmd.getCommandArray()[2], Normalizer.Form.NFKC);
		fragment = fragment.toLowerCase();

		BcDiceApiService bcDiceApi = BcDiceApiService.getInstance();
		Map<String, String> systemNames = bcDiceApi.getSystemNames();
		List<String> resultList = new ArrayList<>();
		for (Entry<String, String> entry : systemNames.entrySet()) {
			String systemId = Normalizer.normalize(entry.getKey(), Normalizer.Form.NFKC);
			String systemName = Normalizer.normalize(entry.getValue(), Normalizer.Form.NFKC);

			if (systemId.toLowerCase().contains(fragment) ||
					systemName.toLowerCase().contains(fragment)) {
				resultList.add(entry.getValue());
			}
		}

		if (resultList.isEmpty()) {
			DiscordUtil.sendMessage("指定された名前で見つかりませんでした。" + dmd.getCommandArray()[2], dmd);
		} else {
			String message = "■システム名（検索結果）：\n";
			for (String systemName : resultList) {
				message += systemName + "\n";
			}
			DiscordUtil.sendMessage(message, dmd);
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("検索したい文字列が設定されていません。/search systemname <システム名の一部>で発言してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdicesearch <システム名の一部（ビガミ、shinobi）など> (旧コマンド：/search systemname)",
				"ダイスボットで指定できるシステム名を検索する。（部分一致検索）");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption systemName = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "システム名の一部",
				"BCDiceのシステム名（ソード、シノビなど）を指定してください。（部分一致検索）", true);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("bcdicesetで指定するためのシステム名のフルネームを検索します。")
				.addOption(systemName)
				;
	}

	@Override
	public String getCommandName() {
		return "bcdicesearch";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String systemName = options.get(0).getStringValue().orElse("");

		String commandText = "/search systemname " + systemName;

		dmd.setText(commandText);

		execute(dmd);
	}

}
