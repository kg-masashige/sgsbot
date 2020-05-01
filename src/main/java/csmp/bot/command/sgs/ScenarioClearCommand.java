package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;

/**
 * シナリオ情報クリアコマンド.
 * @author kgmas
 *
 */
public class ScenarioClearCommand implements IDiscordCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().equals("/sgsclear")) {
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
		Server guild = dmd.getGuild();
		Map<Object, Object> secretMap = CsmpService.getInstance().getGuildScenarioInfo().remove(guild.getId());
		if (secretMap == null) {
			dmd.getChannel().sendMessage("シナリオ情報が設定されていません。");
			return;
		}
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		List<ServerTextChannel> channelList = dmd.getGuild().getTextChannels();
		List<Role> roleList = guild.getRoles();
		for (ServerTextChannel channel : channelList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpService.text(pcInfo, "name");
				if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
					channel.delete().get();
				}
			}
		}
		for (Role role : roleList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpService.text(pcInfo, "name");
				if (roleName.equals(role.getName())) {
					role.delete();
					break;
				}
			}
		}
		dmd.getChannel().sendMessage("役職とチャンネルを削除しました。");


	}
	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgsclear",
				"シナリオシートの情報、チャンネル、権限を削除する。");
	}

}
