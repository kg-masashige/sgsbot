package csmp.bot.command.bcdice;

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
public class BcDiceGetSystemInfoCommand implements IDiscordCommand, IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/get systeminfo ")) {
			return true;
		}

		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length >= 3) {
			String targetSystem = dmd.getCommandArray()[2];
			for (int i = 3; i < dmd.getCommandArray().length; i++) {
				targetSystem += " " + dmd.getCommandArray()[i];
			}
			Map<String, String> systemNames = BcDiceApiService.getInstance().getSystemNames();
			if (systemNames.containsKey(targetSystem)) {
				return true;
			}
			if (systemNames.containsValue(targetSystem)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String targetSystem = dmd.getCommandArray()[2];
		BcDiceApiService bcDiceApi = BcDiceApiService.getInstance();

		Map<String, String> systemNames = bcDiceApi.getSystemNames();
		if (!systemNames.containsKey(targetSystem)) {
			for (Entry<String, String> entry : systemNames.entrySet()) {
				if (targetSystem.equals(entry.getValue())) {
					targetSystem = entry.getKey();
					break;
				}
			}
		}

		String systemInfo = bcDiceApi.getSystemInfoMessage(targetSystem);

		DiscordUtil.sendMessage("システム：" + systemNames.get(targetSystem) + "(" + targetSystem + ")\n"
				+ systemInfo, dmd);

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length >= 3) {
			DiscordUtil.sendMessage("指定されたシステムがダイスボットに存在しません。：" + dmd.getCommandArray()[2], dmd);
		} else {
			DiscordUtil.sendMessage("「/get systeminfo 確認したいシステム名」で発言してください。", dmd);
		}

	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdiceget <確認したいシステム名> (旧コマンド：/get systeminfo)",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）のコマンド一覧を表示する。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption systemName = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "システム名",
				"BCDiceのシステム名（ソード・ワールド2.5、シノビガミなど）を指定してください。", true);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("指定したシステム名のコマンド一覧を自分だけに表示します。")
				.addOption(systemName)
				;
	}

	@Override
	public String getCommandName() {
		return "bcdiceget";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String systemName = options.get(0).getStringValue().orElse("");

		String commandText = "/get systeminfo " + systemName;

		dmd.setText(commandText);

		execute(dmd);

	}

}
