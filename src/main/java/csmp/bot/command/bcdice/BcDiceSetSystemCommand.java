package csmp.bot.command.bcdice;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * ダイスボットシステム設定コマンド.
 * @author kgmas
 *
 */
public class BcDiceSetSystemCommand implements IDiscordCommand {

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
				"/set system <指定したいシステム名>",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）を設定する。");
	}

}
