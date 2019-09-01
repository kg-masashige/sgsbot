package csmp.bot.command.schedule;

import java.util.Map;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;

/**
 * スケジュール削除コマンド.
 * @author kgmas
 *
 */
public class ScheduleDeleteCommand extends DiscordCommandBase {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
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
	public void execute(DiscordMessageData dmd) {

		String dateArray = dmd.getCommandArray()[1];
		if (!"all".equalsIgnoreCase(dateArray)) {
			dateArray = DateUtil.toFormatDateArray(dmd.getCommandArray()[1]);
		}
		Map<String, Object> result = CsmpService.deleteSchedule(dmd.getGuild().getId().asString(), dateArray);
		if (result != null) {
			dmd.getChannel().createMessage(dateArray + "を削除しました。").block();
		} else {
			dmd.getChannel().createMessage("予定日の削除に失敗しました。").block();
		}
	}

	@Override
	public void help(DiscordMessageData dmd) {
		dmd.getChannel().createMessage("コマンドは「/schedel <日付（一度に複数指定する場合はカンマ区切り。全て削除ならallを指定。）>」と入力してください。").block();
	}

}
