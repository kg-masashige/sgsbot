package csmp.bot.command.character;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;

public class LinkCharacterSheetCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}

		if (dmd.getText().startsWith("/link http")) {
			return true;
		}

		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}


	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String sheetUrl = dmd.getCommandArray()[1];

		String webhookUrl = DiscordUtil.getWebhookUrl(dmd);

		if (webhookUrl == null) {
			return;
		}

		Map<String, Object> result = CsmpService.getInstance().registerCharacterSheet(webhookUrl, sheetUrl);
		if (result != null && "ok".equals(result.get("result"))) {
			dmd.getChannel().sendMessage("キャラクターシートを登録しました。キャラクターシートでDRボタンを押してください。");

		} else {
			dmd.getChannel().sendMessage("キャラクターシートの登録に失敗しました。再度コマンドを実行してください。");

		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/link <キャラクターシートURL>",
				"キャラクターシート上でDRボタンを押した時に結果をこのチャンネルに反映させる。");
	}

}
