package csmp.bot.command.schedule;

import java.util.List;
import java.util.Map;

import org.javacord.api.entity.server.Server;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;

/**
 * スケジュール表示コマンド.
 * @author kgmas
 *
 */
public class ScheduleShowCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().equals("/scheshow")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) {

		dmd.getChannel().sendMessage("このコマンドは削除予定機能です。2021年12月に削除します。引き続き日程調整を行いたい方はデイコードをご利用ください。");

		Server guild = dmd.getGuild();
		Map<String, Object> guildScheduleInfo = CsmpService.getInstance().getGuildScheduleInfo(guild.getIdAsString());
		if (guildScheduleInfo == null) {
			dmd.getChannel().sendMessage("セッション予定の取得に失敗しました。登録されていません。");
			return;
		}

		// 日付リスト、メッセージ、次回日程
		List<String> dateList = (List<String>)guildScheduleInfo.get("dateList");
		String messageText = (String)guildScheduleInfo.get("message");
		String text = "予定日は：\r\n";
		for (String date : dateList) {
			text += date + "\r\n";
		}
		if (messageText != null) {
			text += "\r\nリマインドメッセージ：" + messageText;
		}

		dmd.getChannel().sendMessage(text);
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/scheshow",
				"登録済のセッション予定日を表示する。（2021年12月削除予定機能）");
	}

}
