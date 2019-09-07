package csmp.bot.command.schedule;

import java.util.List;
import java.util.Map;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import discord4j.core.object.entity.Guild;

/**
 * スケジュール表示コマンド.
 * @author kgmas
 *
 */
public class ScheduleShowCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
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

		Guild guild = dmd.getGuild();
		Map<String, Object> guildScheduleInfo = CsmpService.getGuildScheduleInfo(guild.getId().asString());
		if (guildScheduleInfo == null) {
			dmd.getChannel().createMessage("セッション予定の取得に失敗しました。登録されていません。").block();
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

		dmd.getChannel().createMessage(text).block();
	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/scheshow",
				"登録済のセッション予定日を表示する。");
	}

}
