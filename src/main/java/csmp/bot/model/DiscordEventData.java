package csmp.bot.model;

import java.util.Map;

import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerEvent;

public class DiscordEventData {

	public DiscordEventData(ServerEvent event, boolean addFlag) {
		this.guild = event.getServer();
		this.event = event;
		this.addFlag = addFlag;
	}

	/**
	 * 追加フラグ
	 */
	private boolean addFlag = false;

	/**
	 * ギルド.
	 */
	private Server guild = null;

	/**
	 * 発生イベント
	 */
	private ServerEvent event = null;

	/**
	 * 取得データ
	 */
	private Map<String, Object> data = null;

	/**
	 * @return guild
	 */
	public Server getGuild() {
		return guild;
	}

	/**
	 * @return event
	 */
	public ServerEvent getEvent() {
		return event;
	}

	/**
	 * @return data
	 */
	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * @param data セットする data
	 */
	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	/**
	 * @return addFlag
	 */
	public boolean isAddFlag() {
		return addFlag;
	}

}
