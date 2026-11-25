package csmp.bot.command.bcdice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * チャンネルでプロットされた値を公開するコマンド.
 * @author kgmas
 *
 */
public class PlotOpenCommand implements ISlashCommand {

	/**
	 * チャンネルID → (ユーザーID → プロット)
	 */
	private static final Map<Long, Map<Long, Plot>> plotMap = new ConcurrentHashMap<>();

	private record Plot(String displayName, String value) {
	}

	/**
	 * プロット値を設定する. 同じユーザーが再度プロットした場合は上書きする.
	 * @param channelId チャンネルID
	 * @param userId ユーザーID
	 * @param displayName 表示名
	 * @param value プロット値
	 */
	public static void setPlot(long channelId, long userId, String displayName, String value) {
		Map<Long, Plot> map = plotMap.computeIfAbsent(channelId, key -> new LinkedHashMap<>());
		synchronized (map) {
			map.put(userId, new Plot(displayName, value));
		}
	}

	/**
	 * チャンネルのプロット値を取り出して消去する.
	 * @param channelId チャンネルID
	 * @return 公開するメッセージ. プロットされていない場合はnull.
	 */
	public static String takePlots(long channelId) {
		Map<Long, Plot> map = plotMap.remove(channelId);
		if (map == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder("■プロット");
		synchronized (map) {
			if (map.isEmpty()) {
				return null;
			}
			for (Plot plot : map.values()) {
				sb.append("\n").append(plot.displayName()).append(":").append(plot.value());
			}
		}
		return sb.toString();
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String message = takePlots(event.getChannelIdLong());
		if (message == null) {
			event.reply("プロットされていません。").queue();
			return;
		}
		event.reply("プロット値を公開します。").queue();
		DiscordUtil.sendToChannel(event.getChannel(), message);
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/openplot",
				"参加者がこのチャンネルでプロットした値を公開する。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "プロットされた値を公開します。")
				.setContexts(InteractionContextType.GUILD);
	}

	@Override
	public String getCommandName() {
		return "openplot";
	}

}
