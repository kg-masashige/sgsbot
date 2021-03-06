package csmp.bot.event.schedule;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;

import csmp.bot.event.IDiscordEvent;
import csmp.bot.model.DiscordEventData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;

public class ScheduleMemberChangeEvent implements IDiscordEvent {

	private static Map<String, String> guildExecutionMap = new ConcurrentHashMap<>();

	private static synchronized boolean checkGuildId(String guildId) {
		boolean isExecution = guildExecutionMap.containsKey(guildId);
		if (!isExecution) {
			guildExecutionMap.put(guildId, guildId);
			return true;
		}
		return false;
	}

	private static synchronized void removeGuildId(String guildId) {
		guildExecutionMap.remove(guildId);
	}

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void execute(DiscordEventData ded) throws InterruptedException, ExecutionException {
		// デイコードから情報を取得する。
		String guildId = ded.getGuild().getIdAsString();

		if (!checkGuildId(guildId)) {
			return;
		}
		try {
			int count = 0;
			while (count < 2) {
				Map<String, Object> sessionMap = CsmpService.getInstance().getScheduleMemberIdMap(guildId);
				if (sessionMap == null || sessionMap.isEmpty()) {
					return;
				}
				Map<String, Map<String, List<String>>> updateSessionMap = getUpdateSessionMap(ded, guildId, sessionMap);
				if (updateSessionMap.isEmpty()) {
					return;
				}
				logger.info(updateSessionMap);
				CsmpService.getInstance().updateScheduleMembers(updateSessionMap);

				// 1回は追加で実施し、セッション情報との差異がないかのチェックを行う。
				count++;
			}

		} finally {
			removeGuildId(guildId);
		}

	}

	/**
	 * セッションごとの更新されたメンバー情報の取得.
	 * @param ded
	 * @param guildId
	 * @param sessionMap
	 * @return
	 */
	private Map<String, Map<String, List<String>>> getUpdateSessionMap(DiscordEventData ded, String guildId,
			Map<String, Object> sessionMap) {
		Map<String, Map<String, List<String>>> updateSessionMap = new HashMap<>();
		for (Entry<String, Object> entry : sessionMap.entrySet()) {
			String id = entry.getKey();
			int qIndex = id.indexOf("?");
			if ( qIndex > 0) {
				id = id.substring(0, qIndex);
			}

			String entryGuildId = null;
			String channelId = null;
			String roleId = null;

			if (id.contains("&")) {
				String[] ids = id.split("&");
				entryGuildId = ids[0];
				roleId = ids[1];
			} else {
				entryGuildId = id;
			}

			if (entryGuildId.contains("#")) {
				String[] ids = entryGuildId.split("#");
				entryGuildId = ids[0];
				channelId = ids[1];
			}

			ServerTextChannel stc = null;
			if (channelId != null) {
				stc = ded.getGuild().getTextChannelById(channelId).orElse(null);
				if (stc == null) {
					continue;
				}
			}
			Role role = null;
			if (roleId != null) {
				role = ded.getGuild().getRoleById(roleId).orElse(null);
				if (role == null) {
					continue;
				}
			}

			Map<String, String> currentMemberIdMap = DiscordUtil.getMemberIdMap(ded.getGuild(), stc, role);
			if (currentMemberIdMap.isEmpty()) {
				// 現在のメンバーIDマップが取得できない場合、何もせずに終える。
				return updateSessionMap;
			}

			Map<String, String> serverMemberIdMap = (Map<String, String>) entry.getValue();
			List<String> updateMemberIdNameList = new ArrayList<>();
			List<String> deleteMemberIdList = new ArrayList<>();
			int count = serverMemberIdMap.size();
			for (Entry<String, String> currentMember : currentMemberIdMap.entrySet()) {
				String userId = currentMember.getKey();
				String serverUserName = serverMemberIdMap.get(userId);
				if (serverMemberIdMap.containsKey(userId)) {
					if (!currentMember.getValue().equals(serverUserName)) {
						updateMemberIdNameList.add(userId + ":" + currentMember.getValue());
					}
				} else {
					updateMemberIdNameList.add(userId + ":" + currentMember.getValue());
					count++;
				}
			}

			for (Entry<String, String> serverMember : serverMemberIdMap.entrySet()) {
				String userId = serverMember.getKey();
				if (!currentMemberIdMap.containsKey(userId)) {
					deleteMemberIdList.add(userId);
					count--;
				}
			}

			if (updateMemberIdNameList.isEmpty() && deleteMemberIdList.isEmpty()) {
				continue;
			}

			Map<String, List<String>> updateMap = new HashMap<>();
			updateMap.put("update", updateMemberIdNameList);
			if (!ded.isAddFlag()) {
				// 追加フラグがtrueの場合は削除イベントを実行しない。
				updateMap.put("delete", deleteMemberIdList);
			}
			updateSessionMap.put(entry.getKey(), updateMap);
		}
		return updateSessionMap;
	}

}
