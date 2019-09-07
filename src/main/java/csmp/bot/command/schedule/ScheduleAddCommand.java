package csmp.bot.command.schedule;

import java.util.List;
import java.util.Map;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.Webhook;

/**
 * スケジュール追加コマンド.
 * @author kgmas
 *
 */
public class ScheduleAddCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
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
	public void execute(DiscordMessageData dmd) {

		String dateArray = DateUtil.toFormatDateArray(dmd.getCommandArray()[1]);
		String messageText = "";
		if (dmd.getCommandArray().length > 2) {
			messageText = dmd.getCommandArray()[2];
		}

		TextChannel tc = dmd.getChannel();
		Guild guild = dmd.getGuild();
		List<Webhook> webhookList = tc.getWebhooks().collectList().block();
		Webhook webhook;
		if (webhookList == null || webhookList.isEmpty()) {
			webhook = tc.createWebhook(spec -> {
				spec.setName(guild.getName());
			}).block();
		} else {
			webhook = webhookList.get(0);
		}
		String webhookUrl = "https://discordapp.com/api/webhooks/"
				+ webhook.getId().asString() + "/" + webhook.getToken();


		Map<String, Object> result = CsmpService.registerSchedule(guild.getId().asString(), webhookUrl, dateArray, messageText);
		if (result != null) {
			tc.createMessage(dateArray + "を登録しました。").block();
			System.out.println("サーバ：" + guild.getName() + "　登録日：" + dateArray);
		} else {
			tc.createMessage("予定日の登録に失敗しました。").block();
			System.out.println("サーバ：" + guild.getName() + "　登録失敗：" + dateArray);
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().createMessage("コマンドは「/scheadd <日付（一度に複数指定する場合はカンマ区切り）> <リマインドメッセージ（オプション）>」と入力してください。").block();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData("/scheadd <日付> <リマインドメッセージ（オプション）>",
				"セッション予定日になったらリマインドメッセージを飛ばす。",
				"日付を指定する時はカンマ区切りで複数指定可能。例：/scheadd 9/1,9/2");
	}


}
