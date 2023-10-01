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
 * ダイスボットシステム設定コマンド.
 * @author kgmas
 *
 */
public class BcDiceSetSystemCommand implements IDiscordCommand, IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/set system ")) {
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

		bcDiceApi.putGuildSystem(dmd.getGuild().getId(), targetSystem);
		dmd.getChannel().sendMessage("ダイスボットを" + systemNames.get(targetSystem) + "に設定しました。");

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length >= 3) {
			dmd.getChannel().sendMessage("指定されたシステムがダイスボットに存在しません。：" + dmd.getCommandArray()[2]);
		} else {
			dmd.getChannel().sendMessage("「/set system 指定したいシステム名」で発言してください。");
		}

	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/bcdiceset <指定したいシステム名> (旧コマンド：/set system)",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）を設定する。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption systemName = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "システム名",
				"BCDiceのシステム名（ソード・ワールド2.5、シノビガミなど）を指定してください。", true);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("BCDiceで判定する際のシステム名を設定します。")
				.addOption(systemName)
				;
	}

	@Override
	public String getCommandName() {
		return "bcdiceset";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String systemName = options.get(0).getStringValue().orElse("");

		String commandText = "/set system " + systemName;

		dmd.setText(commandText);

		if (checkInput(dmd)) {
			DiscordUtil.sendMessage(systemName + "を設定します。", dmd);
			execute(dmd);
		} else {
			DiscordUtil.sendMessage(systemName + "が見つかりません。/bcdicesearchで正しいシステム名を検索して設定してください。", dmd);
		}


	}

}
