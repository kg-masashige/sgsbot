package csmp.bot.command.schedule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;

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
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {

		String dateArray = DateUtil.toFormatDateArray(dmd.getCommandArray()[1]);
		String messageText = "";
		if (dmd.getCommandArray().length > 2) {
			messageText = dmd.getCommandArray()[2];
		}

		ServerTextChannel tc = (ServerTextChannel)dmd.getChannel();
		Server guild = dmd.getGuild();
		List<Webhook> webhookList = tc.getWebhooks().get();
		Webhook webhook;
		if (webhookList == null || webhookList.isEmpty()) {
			webhook = new WebhookBuilder(tc)
					.setName(guild.getName())
					.create().get();
		} else {
			webhook = webhookList.get(0);
		}


		String webhookUrl = "https://discordapp.com/api/webhooks/"
				+ webhook.getIdAsString() + "/" + webhook.getToken();

		Map<String, Object> result = CsmpService.registerSchedule(guild.getIdAsString(), webhookUrl, dateArray, messageText);
		if (result != null) {
			tc.sendMessage(dateArray + "を登録しました。");
			System.out.println("サーバ：" + guild.getName() + "　登録日：" + dateArray);
		} else {
			tc.sendMessage("予定日の登録に失敗しました。");
			System.out.println("サーバ：" + guild.getName() + "　登録失敗：" + dateArray);
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
