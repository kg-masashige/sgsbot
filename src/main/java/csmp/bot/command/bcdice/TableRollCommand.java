package csmp.bot.command.bcdice;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * テーブル実行コマンド.
 * @author kgmas
 *
 */
public class TableRollCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		Map<String, Map<String, Object>> tableInfo = BcDiceApiService.getInstance().getTableInfo(dmd.getGuild().getId());
		if (tableInfo == null) {
			return false;
		}
		return tableInfo.containsKey(dmd.getText());
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		BcDiceApiService service = BcDiceApiService.getInstance();
		Map<String, Map<String, Object>> tableInfo = service.getTableInfo(dmd.getGuild().getId());
		Map<String, Object> tableDetail = tableInfo.get(dmd.getText());
		String dice = (String)tableDetail.get("dice");
		String title = (String)tableDetail.get("title");
		Map<String, String> resultMap = (Map<String, String>)tableDetail.get("result");

		String rollResult = service.rollDice(dice, dmd.getGuild().getId());
		if (rollResult == null) {
			String system = service.getGuildSystem(dmd.getGuild().getId());
			dmd.getChannel().sendMessage("ダイスロールが失敗しました。システム：" + system + " コマンド：" + dice);
			return;
		}

		String[] rollResultArray = rollResult.split(" ", -1);
		String value = rollResultArray[rollResultArray.length - 1];
		String explain = resultMap.get(value);
		if (explain == null) {
			explain = "";
		}

		String message = title + "(" + value + ") → " + explain.replaceAll("\\\\n", "\n");
		dmd.getChannel().sendMessage(
				new EmbedBuilder().setAuthor(dmd.getMessage().getMessageAuthor())
					.addField(dmd.getText(), message)
				);
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return null;
	}

}
