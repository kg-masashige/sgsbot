package csmp.utl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;

public class DiscordUtil {
	/**
	 * プライベートチャンネル用の権限を設定する.
	 * @param role 役割
	 * @param guild サーバ
	 * @return 権限情報
	 */
	public static Set<PermissionOverwrite> getPrivateChannelPermission(Role role, Guild guild) {
		Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();
		PermissionSet viewPs = PermissionSet.of(Permission.VIEW_CHANNEL);
		PermissionOverwrite po = PermissionOverwrite.forRole(role.getId(), viewPs, PermissionSet.none());
		PermissionOverwrite everyonePO = PermissionOverwrite.forRole(guild.getEveryoneRole().block().getId(), PermissionSet.none(), viewPs);
		permissionOverwrites.add(po);
		permissionOverwrites.add(everyonePO);

		return permissionOverwrites;
	}

	/**
	 * チャンネル名を指定してテキストチャンネルを取得する.
	 * @param guild サーバ
	 * @param name 名前
	 * @return
	 */
	public static TextChannel getTextChannelByName(Guild guild, String name) {
		List<GuildChannel> channels = guild.getChannels().collectList().block();
		for (GuildChannel channel : channels) {
			if (name.toLowerCase().equals(channel.getName().toLowerCase())) {
				if (channel instanceof TextChannel) {
					return (TextChannel)channel;
				}
			}
		}
		return null;
	}


}
