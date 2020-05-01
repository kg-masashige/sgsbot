package csmp.bot.command.bcdice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.user.User;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

/**
 * 参加者全員のプロット（botに対するダイレクトメッセージ）を表示するコマンド.
 * @author kgmas
 *
 */
public class PlotOpenCommand implements IDiscordCommand {

	private static Map<Long, Long> userMessageIdMap = new ConcurrentHashMap<>();

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().equalsIgnoreCase("/plot open") || dmd.getText().equalsIgnoreCase("/plot who")) {
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

		List<String> list = new ArrayList<>();
		for (User user : dmd.getGuild().getEveryoneRole().getUsers()) {
			user.getPrivateChannel().ifPresent(channel ->
				{
					try {
						// 最新のメッセージを取得する。
						// 最新のメッセージがユーザIDをキーにした最新のメッセージIDと一致したら無視する。
						channel.getMessages(1).get().getNewestMessage().ifPresent(message -> {
							Long messageId = userMessageIdMap.get(user.getId());
							if (messageId == null || messageId != message.getId()) {
								if ("who".equals(dmd.getCommandArray()[1])) {
									list.add(user.getDisplayName(dmd.getGuild()) + " : プロット済");
								} else {
									list.add(user.getDisplayName(dmd.getGuild()) + " : " + message.getContent());
									userMessageIdMap.put(user.getId(), message.getId());

								}
							}
						});
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
			);
		}

		String text = "■プロット";
		if (!list.isEmpty()) {
			for (String message : list) {
				text += "\r\n" + message;
			}
		} else {
			text += "\r\n誰もプロットしていません。";
		}
		dmd.getChannel().sendMessage(text);

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData(
				"/plot open",
				"参加者がbotへ送ったダイレクトメッセージの最新をすべて公開する。"));
		list.add(new CommandHelpData(
				"/plot who",
				"参加者のうち誰がプロットしているかを確認する。"));

		return new CommandHelpData(list);
	}

}
