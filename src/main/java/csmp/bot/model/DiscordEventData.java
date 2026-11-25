package csmp.bot.model;

import net.dv8tion.jda.api.entities.Guild;

/**
 * サーバーで発生したイベントの情報.
 */
public class DiscordEventData {

	public DiscordEventData(Guild guild, boolean addFlag) {
		this.guild = guild;
		this.addFlag = addFlag;
	}

	/**
	 * 追加フラグ
	 */
	private final boolean addFlag;

	/**
	 * ギルド.
	 */
	private final Guild guild;

	/**
	 * @return guild
	 */
	public Guild getGuild() {
		return guild;
	}

	/**
	 * @return addFlag
	 */
	public boolean isAddFlag() {
		return addFlag;
	}

}
