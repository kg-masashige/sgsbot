package csmp.bot.controller;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csmp.bot.command.ISlashCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.event.IDiscordEvent;
import csmp.bot.model.DiscordEventData;
import net.arnx.jsonic.JSON;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DiscordBotController extends ListenerAdapter {

	/**
	 * エラー時にユーザーへ返すメッセージ.
	 */
	private static final String ERROR_MESSAGE = "エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。";

	/**
	 * コマンド名 → コマンド.
	 */
	private final Map<String, ISlashCommand> commandMap = new LinkedHashMap<>();

	/**
	 * イベントリスト
	 */
	private final List<IDiscordEvent> eventList = new ArrayList<>();

	/**
	 * トークン.
	 */
	private final String token;

	/**
	 * シャード数
	 */
	private int totalShards = 0;

	/**
	 * スタートシャードNo
	 */
	private int startShardNo = -1;

	/**
	 * エンドシャードNo
	 */
	private int endShardNo = -1;

	/**
	 * ログインメッセージ
	 */
	private String joinMessage = "";

	/**
	 * インテントのタイプ
	 */
	private IntentType intentType = IntentType.COMMANDONLY;

	/**
	 * コマンド登録済みフラグ.
	 * グローバルコマンドのため、プロセス内で1回だけ登録する。
	 */
	private final AtomicBoolean commandsRegistered = new AtomicBoolean(false);

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * コンストラクタ.
	 * @param commandList コマンドリスト（ヘルプは自動で先頭に追加する）
	 * @param eventTriggerList イベントリスト
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<ISlashCommand> commandList,
			List<IDiscordEvent> eventTriggerList, String token) {

		this.token = token;

		HelpCommand helpCommand = new HelpCommand(commandList);
		commandMap.put(helpCommand.getCommandName(), helpCommand);
		for (ISlashCommand command : commandList) {
			commandMap.put(command.getCommandName(), command);
		}

		if (eventTriggerList != null) {
			eventList.addAll(eventTriggerList);
		}
	}

	/**
	 * 登録するコマンド定義の一覧を作成する.
	 * @return コマンド定義
	 */
	public List<SlashCommandData> createCommandDataList() {
		List<SlashCommandData> list = new ArrayList<>();
		for (ISlashCommand command : commandMap.values()) {
			list.add(command.createCommandData());
		}
		return list;
	}

	/**
	 * 実行メソッド.
	 *
	 * @param joinMessage 接続時メッセージ
	 * @param intentType 許可するintentのタイプ
	 */
	public void execute(String joinMessage, IntentType intentType) {
		this.joinMessage = joinMessage;
		this.intentType = intentType;

		// 日程調整のメンバー取得のため、全メンバーをキャッシュする（javacord版と同じ挙動）。
		DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createLight(token, intentType.getIntents())
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.setChunkingFilter(ChunkingFilter.ALL)
				.enableCache(CacheFlag.MEMBER_OVERRIDES)
				// 外部APIの呼び出しで待つことがあるため、イベントは仮想スレッドで処理する。
				.setEventPool(Executors.newVirtualThreadPerTaskExecutor(), true)
				.addEventListeners(this);

		if (startShardNo >= 0 && totalShards == 0) {
			// 範囲指定のときは総シャード数が必要なため、推奨数を取得する。
			totalShards = retrieveRecommendedShards();
		}
		if (totalShards > 0) {
			builder.setShardsTotal(totalShards);
		}
		if (startShardNo >= 0) {
			if (endShardNo < 0) {
				endShardNo = totalShards - 1;
			}
			builder.setShards(startShardNo, endShardNo);
		}

		builder.build();
	}

	/**
	 * Discordの推奨シャード数を取得する.
	 * @return 推奨シャード数
	 */
	private int retrieveRecommendedShards() {
		Request request = new Request.Builder()
				.url("https://discord.com/api/v10/gateway/bot")
				.addHeader("Authorization", "Bot " + token)
				.get().build();
		try (Response response = new OkHttpClient().newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IllegalStateException("推奨シャード数の取得に失敗しました。HTTP " + response.code());
			}
			Map<String, Object> map = JSON.decode(response.body().string());
			return ((Number) map.get("shards")).intValue();
		} catch (java.io.IOException e) {
			throw new IllegalStateException("推奨シャード数の取得に失敗しました。", e);
		}
	}

	/**
	 * @param totalShards 合計シャード数
	 */
	public void setTotalShards(int totalShards) {
		this.totalShards = totalShards;
	}

	/**
	 * @param start シャード開始No
	 */
	public void setStart(int start) {
		this.startShardNo = start;
	}

	/**
	 * @param end シャード終了No
	 */
	public void setEnd(int end) {
		this.endShardNo = end;
	}

	@Override
	public void onReady(ReadyEvent event) {
		logger.info("Botの起動完了. bot名:" + event.getJDA().getSelfUser().getName()
				+ " shard:" + event.getJDA().getShardInfo().getShardString());

		if (commandsRegistered.compareAndSet(false, true)) {
			event.getJDA().updateCommands().addCommands(createCommandDataList()).queue(
					commands -> logger.info("スラッシュコマンドを登録しました。件数:" + commands.size()),
					e -> {
						logger.error("スラッシュコマンドの登録に失敗しました。", e);
						commandsRegistered.set(false);
					});
		}
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		ISlashCommand command = commandMap.get(event.getName());
		if (command == null) {
			return;
		}
		if (event.getGuild() == null) {
			event.reply("サーバー内で実行してください。").setEphemeral(true).queue();
			return;
		}
		try {
			command.execute(event);
		} catch (Throwable e) {
			handleError(event, "error command:" + event.getCommandString(), e);
		}
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event) {
		String modalId = event.getModalId();
		int index = modalId.indexOf(':');
		if (index < 0) {
			return;
		}
		ISlashCommand command = commandMap.get(modalId.substring(0, index));
		if (command == null || event.getGuild() == null) {
			return;
		}
		try {
			command.executeModal(event);
		} catch (Throwable e) {
			handleError(event, "error modal:" + modalId, e);
		}
	}

	@Override
	public void onGuildMemberJoin(GuildMemberJoinEvent event) {
		triggerEvent(event.getGuild(), true);
	}

	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
		triggerEvent(event.getGuild(), false);
	}

	@Override
	public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
		triggerEvent(event.getGuild(), true);
	}

	@Override
	public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
		triggerEvent(event.getGuild(), false);
	}

	@Override
	public void onPermissionOverrideCreate(PermissionOverrideCreateEvent event) {
		triggerEvent(event.getGuild(), false);
	}

	@Override
	public void onPermissionOverrideUpdate(PermissionOverrideUpdateEvent event) {
		triggerEvent(event.getGuild(), false);
	}

	@Override
	public void onPermissionOverrideDelete(PermissionOverrideDeleteEvent event) {
		triggerEvent(event.getGuild(), false);
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		if (!intentType.isMemberEventEnabled()) {
			return;
		}
		var systemChannel = event.getGuild().getSystemChannel();
		if (systemChannel != null && systemChannel.canTalk()) {
			systemChannel.sendMessage(joinMessage).queue();
		}
	}

	private void triggerEvent(Guild guild, boolean addFlag) {
		if (!intentType.isMemberEventEnabled()) {
			return;
		}
		DiscordEventData ded = new DiscordEventData(guild, addFlag);
		try {
			for (IDiscordEvent event : eventList) {
				event.execute(ded);
			}
		} catch (Exception e) {
			logger.error("error event:" + guild.getId() + ":" + guild.getName(), e);
		}
	}

	private void handleError(IReplyCallback event, String logMessage, Throwable e) {
		logger.error(logMessage, e);
		try {
			if (event.isAcknowledged()) {
				event.getHook().sendMessage(ERROR_MESSAGE).setEphemeral(true).queue();
			} else {
				event.reply(ERROR_MESSAGE).setEphemeral(true).queue();
			}
		} catch (Exception ex) {
			logger.error("エラーメッセージの送信に失敗しました。", ex);
		}
	}

	public enum IntentType {
		/**
		 * メンバー変更イベントあり（テキストコマンド廃止に伴い MEMBER_CHANGE と同じ）.
		 */
		ALL(true),
		/**
		 * メンバー変更イベントあり.
		 */
		MEMBER_CHANGE(true),
		/**
		 * スラッシュコマンドのみ.
		 */
		COMMANDONLY(false);

		private final boolean memberEventEnabled;

		private IntentType(boolean memberEventEnabled) {
			this.memberEventEnabled = memberEventEnabled;
		}

		/**
		 * @return 利用するGateway Intent. GUILDS はJDAが常に有効にする。
		 */
		public EnumSet<GatewayIntent> getIntents() {
			return EnumSet.of(GatewayIntent.GUILD_MEMBERS);
		}

		/**
		 * @return メンバー変更イベントとサーバー参加時の挨拶を処理する場合はtrue
		 */
		public boolean isMemberEventEnabled() {
			return memberEventEnabled;
		}

		/**
		 * 環境変数の値から取得する. 未設定の場合は COMMANDONLY.
		 * @param value 環境変数の値
		 * @return インテントのタイプ
		 */
		public static IntentType of(String value) {
			if (value == null || value.isBlank()) {
				return COMMANDONLY;
			}
			return valueOf(value.trim());
		}
	}

}
