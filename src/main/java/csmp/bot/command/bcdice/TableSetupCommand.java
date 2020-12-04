package csmp.bot.command.bcdice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * テーブル追加コマンド.
 * @author kgmas
 *
 */
public class TableSetupCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if ("/sgstable".equals(dmd.getCommandArray()[0])) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length >= 2 && "clear".equals(dmd.getCommandArray()[1])) {
			return true;
		}


		String[] lines = dmd.getText().split("\n", -1);
		if (lines.length <= 1) {
			return false;
		}
		String head = lines[0];
		String[] headColumns = head.split(" ", -1);
		if (headColumns.length != 4) {
			// /sgstable <コマンド> <ダイス> <名前>
			return false;
		}


		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		BcDiceApiService service = BcDiceApiService.getInstance();
		if (dmd.getCommandArray().length >= 2 && "clear".equals(dmd.getCommandArray()[1])) {
			service.setTableInfo(dmd.getGuild().getId(), null);
			dmd.getChannel().sendMessage("ダイスボット表をクリアしました。");
			return;
		}

		Map<String, Map<String, Object>> tableInfo = service.getTableInfo(dmd.getGuild().getId());
		if (tableInfo == null) {
			tableInfo = new ConcurrentHashMap<>();
		}

		String[] lines = dmd.getText().split("\n", -1);
		String head = lines[0];
		String[] headColumns = head.split(" ", -1);

		Map<String, Object> detailMap = new ConcurrentHashMap<>();
		tableInfo.put(headColumns[1], detailMap);

		detailMap.put("dice", headColumns[2]);
		detailMap.put("title", headColumns[3]);

		Map<String, String> resultMap = new ConcurrentHashMap<>();
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			int sepIndex = line.indexOf(":");
			if (sepIndex <= 0) {
				continue;
			}
			String value = line.substring(0, sepIndex).trim();
			String explain = line.substring(sepIndex + 1).trim();
			resultMap.put(value, explain);
		}
		detailMap.put("result", resultMap);

		service.setTableInfo(dmd.getGuild().getId(), tableInfo);

		dmd.getChannel().sendMessage("コマンド：" + headColumns[1] + "に" + headColumns[3] + "を登録しました。");

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage(
				"/sgstableコマンドで登録する時は、1行目に\n" +
				"/sgstable <反応させるコマンド(完全一致)> <ダイス> <名前>\n" +
				"2行目以降に\n" +
				"<ダイスの結果1>:<出力したい値>\n" +
				"<ダイスの結果2>:<出力したい値>\n" +
				"……\n" +
				" (<>は入力しない)\n" +
				"と入力してください。"
				);

	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgstable <コマンド> <ダイス> <タイトル> 2行目以降に<ダイスの結果1>:<出力したい文章>...",
				"コマンドの実行結果を表に当てはめて結果を出力する。");
	}

}
