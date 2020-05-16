package csmp.bot.command.bcdice;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * ダイスボットシステム説明取得コマンド.
 * @author kgmas
 *
 */
public class BcDiceGetSystemInfoCommand implements IDiscordCommand {

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

		String systemInfo = bcDiceApi.getSystemInfo(targetSystem);

		dmd.getChannel().sendMessage("システム：" + systemNames.get(targetSystem) + "(" + targetSystem + ")\n"
				+ systemInfo);

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length >= 3) {
			dmd.getChannel().sendMessage("指定されたシステムがダイスボットに存在しません。：" + dmd.getCommandArray()[2]);
		} else {
			dmd.getChannel().sendMessage("「/get systeminfo 確認したいシステム名」で発言してください。");
		}

	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/get systeminfo <確認したいシステム名>",
				"ダイスボットのシステム（クトゥルフ、シノビガミなど）のコマンド一覧を表示する。");
	}

}
