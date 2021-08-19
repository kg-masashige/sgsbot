package csmp.bot.command.bcdice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.user.User;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.callback.InteractionImmediateResponseBuilder;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;

/**
 * 参加者全員のプロット（botに対するダイレクトメッセージ）を表示するコマンド.
 * @author kgmas
 *
 */
public class PlotOpenCommand implements IDiscordCommand,IDiscordSlashCommand {

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
				"/openplot (旧コマンド：/plot open)",
				"参加者がこのチャンネルでプロットした値を公開する。"));

		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("プロットされた値を公開します。");

	}

	@Override
	public String getCommandName() {
		return "openplot";
	}

	private static Map<Long, Map<Long, String>> plotMap = new ConcurrentHashMap<>();

	public static synchronized void setPlot(long channelId, long userId, String message) {
		Map<Long, String> map = plotMap.get(channelId);
		if (map == null) {
			map = new ConcurrentHashMap<>();
			plotMap.put(channelId, map);
		}
		map.put(userId, message);
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		Map<Long, String> map = plotMap.get(dmd.getChannel().getId());
		InteractionImmediateResponseBuilder responder = dmd.getInteraction().createImmediateResponder();
		if (map == null) {
			responder.setContent("プロットされていません。").respond();
			return;
		}
		responder.setContent("プロット値を公開します。").respond();

		for (User user : dmd.getGuild().getMembers()) {
			String message = map.get(user.getId());
			if (message != null) {
				dmd.getChannel().sendMessage(user.getNickname(dmd.getGuild()).orElse(user.getName()) + ":" + message);
				plotMap.remove(user.getId());
			}
		}
	}

}
