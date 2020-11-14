package csmp.bot.command.schedule;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;
import csmp.utl.DiscordUtil;

/**
 * スケジュール追加コマンド.
 * @author kgmas
 *
 */
public class ScheduleAddCommand implements IDiscordCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	
	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/scheadd")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length < 2) {
			return false;
		}
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {

		String dateArray = DateUtil.toFormatDateArray(dmd.getCommandArray()[1]);
		String messageText = "";
		if (dmd.getCommandArray().length > 2) {
			messageText = dmd.getCommandArray()[2];
		}

		String webhookUrl = DiscordUtil.getWebhookUrl(dmd);
		if (webhookUrl == null) {
			return;
		}

		Map<String, Object> result = CsmpService.getInstance().registerSchedule(dmd.getGuild().getIdAsString(), webhookUrl, dateArray, messageText);
		if (result != null) {
			dmd.getChannel().sendMessage(dateArray + "を登録しました。");
			logger.info("サーバ：" + dmd.getGuild().getName() + "　登録日：" + dateArray);
		} else {
			dmd.getChannel().sendMessage("予定日の登録に失敗しました。");
			logger.info("サーバ：" + dmd.getGuild().getName() + "　登録失敗：" + dateArray);
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("コマンドは「/scheadd <日付（一度に複数指定する場合はカンマ区切り）> <リマインドメッセージ（オプション）>」と入力してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/scheadd <日付> <リマインドメッセージ（オプション）>",
				"セッション予定日になったらリマインドメッセージを飛ばす。",
				"日付を指定する時はカンマ区切りで複数指定可能。例：/scheadd 9/1,9/2");
	}


}
