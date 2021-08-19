package csmp.bot.model;

import java.util.List;

public class ScheduleCommandData {

	/**
	 * スラッシュコマンド.
	 */
	private boolean slashCommand;

	/**
	 * ギルドID(サーバID)
	 */
	private String guildId;
	/**
	 * サーバ名
	 */
	private String serverName;
	/**
	 * Webhook URL
	 */
	private String webhook;
	/**
	 * 作成者のID:名前
	 */
	private String authorIdName;
	/**
	 * ユーザID:名前のリスト
	 */
	private List<String> userIdNameList;
	/**
	 * ロールID
	 */
	private String roleId;
	/**
	 * 強制実行フラグ
	 */
	private boolean force = false;
	/**
	 * @return guildId
	 */
	public String getGuildId() {
		return guildId;
	}
	/**
	 * @param guildId セットする guildId
	 */
	public void setGuildId(String guildId) {
		this.guildId = guildId;
	}
	/**
	 * @return serverName
	 */
	public String getServerName() {
		return serverName;
	}
	/**
	 * @param serverName セットする serverName
	 */
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	/**
	 * @return webhook
	 */
	public String getWebhook() {
		return webhook;
	}
	/**
	 * @param webhook セットする webhook
	 */
	public void setWebhook(String webhook) {
		this.webhook = webhook;
	}
	/**
	 * @return authorIdName
	 */
	public String getAuthorIdName() {
		return authorIdName;
	}
	/**
	 * @param authorIdName セットする authorIdName
	 */
	public void setAuthorIdName(String authorIdName) {
		this.authorIdName = authorIdName;
	}
	/**
	 * @return userIdNameList
	 */
	public List<String> getUserIdNameList() {
		return userIdNameList;
	}
	/**
	 * @param userIdNameList セットする userIdNameList
	 */
	public void setUserIdNameList(List<String> userIdNameList) {
		this.userIdNameList = userIdNameList;
	}
	/**
	 * @return roleId
	 */
	public String getRoleId() {
		return roleId;
	}
	/**
	 * @param roleId セットする roleId
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	/**
	 * @return force
	 */
	public boolean isForce() {
		return force;
	}
	/**
	 * @param force セットする force
	 */
	public void setForce(boolean force) {
		this.force = force;
	}
	/**
	 * @return slashCommand
	 */
	public boolean isSlashCommand() {
		return slashCommand;
	}
	/**
	 * @param slashCommand セットする slashCommand
	 */
	public void setSlashCommand(boolean slashCommand) {
		this.slashCommand = slashCommand;
	}
}
