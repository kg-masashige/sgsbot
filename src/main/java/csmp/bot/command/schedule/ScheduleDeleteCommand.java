package csmp.bot.command.schedule;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;

/**
 * スケジュール削除コマンド.
 * @author kgmas
 *
 */
public class ScheduleDeleteCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/schedel")) {
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

		dmd.getChannel().sendMessage("このコマンドは削除予定機能です。2021年12月に削除します。引き続き日程調整を行いたい方はデイコードをご利用ください。");

		String dateArray = dmd.getCommandArray()[1];
		if (!"all".equalsIgnoreCase(dateArray)) {
			dateArray = DateUtil.toFormatDateArray(dmd.getCommandArray()[1]);
		}
		Map<String, Object> result = CsmpService.getInstance().deleteSchedule(dmd.getGuild().getIdAsString(), dateArray);
		if (result != null) {
			dmd.getChannel().sendMessage(dateArray + "を削除しました。");
		} else {
			dmd.getChannel().sendMessage("予定日の削除に失敗しました。");
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("コマンドは「/schedel <日付（一度に複数指定する場合はカンマ区切り。全て削除ならallを指定。）>」と入力してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/schedel <日付>",
				"指定したセッション予定日を削除する。（2021年12月削除予定機能）",
				"日付を指定する時はカンマ区切りで複数指定可能。allを指定すれば全て削除。例：/schedel 9/1,9/2"
				);
	}
}
