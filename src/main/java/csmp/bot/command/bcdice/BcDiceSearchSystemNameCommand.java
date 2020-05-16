package csmp.bot.command.bcdice;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
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
public class BcDiceSearchSystemNameCommand implements IDiscordCommand {

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
			dmd.getChannel().sendMessage("指定された名前で見つかりませんでした。" + dmd.getCommandArray()[2]);
		} else {
			String message = "■システム名（検索結果）：\n";
			for (String systemName : resultList) {
				message += systemName + "\n";
			}
			dmd.getChannel().sendMessage(message);
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("検索したい文字列が設定されていません。/search systemname <システム名の一部>で発言してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/search systemname <システム名の一部（ビガミ、shinobi）など>",
				"ダイスボットで指定できるシステム名を検索する。（部分一致検索）");
	}

}
