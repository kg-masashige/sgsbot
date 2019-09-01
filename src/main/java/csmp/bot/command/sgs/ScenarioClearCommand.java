package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;

/**
 * シナリオ情報クリアコマンド.
 * @author kgmas
 *
 */
public class ScenarioClearCommand extends DiscordCommandBase {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
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
	public void execute(DiscordMessageData dmd) {
		Guild guild = dmd.getGuild();
		Map<Object, Object> secretMap = CsmpService.getGuildScenarioInfo().remove(guild.getId());
		if (secretMap == null) {
			dmd.getChannel().createMessage("シナリオ情報が設定されていません。").block();
			return;
		}
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		List<GuildChannel> channelList = dmd.getGuild().getChannels().collectList().block();
		List<Role> roleList = guild.getRoles().collectList().block();
		for (GuildChannel channel : channelList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpService.text(pcInfo, "name");
				if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
					channel.delete().block();
				}
			}
		}
		for (Role role : roleList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpService.text(pcInfo, "name");
				if (roleName.equals(role.getName())) {
					role.delete().block();
					break;
				}
			}
		}
		dmd.getChannel().createMessage("役職とチャンネルを削除しました。").block();


	}

}
