package csmp.bot.controller;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.event.IDiscordEvent;
import csmp.bot.model.DiscordEventData;
import csmp.bot.model.DiscordMessageData;

public class DiscordBotController {

	/**
	 * コマンドリスト.
	 */
	private List<IDiscordCommand> commandList;

	/**
	 * イベントリスト
	 */
	private List<IDiscordEvent> eventList;

	/**
	 * トークン.
	 */
	private String token;

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
	 * キャッシュサイズ
	 */
	private int cacheSize = 50;

	/**
	 * キャッシュ保持期間
	 */
	private int cacheStorageTimeInSeconds = 12 * 60 * 60;

	/**
	 * ログインメッセージ
	 */
	private String joinMessage = "";

	/**
	 * メッセージインテントのON/OFF
	 */
	private boolean isMessageIntent = true;

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * コンストラクタ.
	 * @param messageTriggerList コマンドリスト
	 * @param eventTriggerList イベントトリガーコマンドリスト
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<Class<? extends IDiscordCommand>> messageTriggerList,
			List<Class<? extends IDiscordEvent>> eventTriggerList, String token) {

		this.token = token;
		this.commandList = new ArrayList<>();
		List<IDiscordCommand> commandListForHelp = new ArrayList<>();
		for (Class<? extends IDiscordCommand> clazz : messageTriggerList) {
			IDiscordCommand command;
			try {
				command = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			commandListForHelp.add(command);
		}
		HelpCommand hc = new HelpCommand();
		hc.setCommandList(commandListForHelp);
		commandList.add(hc);
		commandList.addAll(commandListForHelp);

		this.eventList = new ArrayList<>();

		if (eventTriggerList == null) {
			return;
		}

		for (Class<? extends IDiscordEvent> clazz : eventTriggerList) {
			IDiscordEvent event;
			try {
				event = clazz.newInstance();
				this.eventList.add(event);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
		}
	}

	/**
	 * 実行メソッド.
	 *
	 * @param joinMessage 接続時メッセージ
	 * @param isMessageIntent MESSAGE_INTENTのON/OFF
	 */
	public void execute(String joinMessage, boolean isMessageIntent) {
		this.joinMessage = joinMessage;
		this.isMessageIntent = isMessageIntent;

		DiscordApiBuilder apiBuilder = new DiscordApiBuilder().setToken(token);
		if (isMessageIntent) {
			apiBuilder.setIntents(
					Intent.DIRECT_MESSAGES,
					Intent.GUILDS,
					Intent.GUILD_MESSAGES,
					Intent.GUILD_MEMBERS,
					Intent.GUILD_WEBHOOKS,
					Intent.GUILD_INTEGRATIONS);
		} else {
			apiBuilder.setIntents(
					Intent.GUILDS,
					Intent.GUILD_MEMBERS,
					Intent.GUILD_WEBHOOKS,
					Intent.GUILD_INTEGRATIONS);
		}

		if (totalShards == 0) {
			apiBuilder = apiBuilder.setRecommendedTotalShards().join();
			totalShards = apiBuilder.getTotalShards();
		} else {
			apiBuilder = apiBuilder.setTotalShards(totalShards);
		}

		int[] array = null;
		if (startShardNo >= 0) {
			if (endShardNo < 0) {
				endShardNo = totalShards - 1;
			}
			// 0～20 : 21個
			// 21～ラスト：21～39

			array = new int[endShardNo - startShardNo + 1];
			for (int i = 0; i < array.length; i++) {
				array[i] = startShardNo + i;
			}
			apiBuilder.loginShards(array)
			.forEach(shardFuture -> shardFuture
					.thenAcceptAsync(this::onShardLogin)
					.exceptionally(ExceptionLogger.get()));

		} else {
			apiBuilder.loginAllShards()
			.forEach(shardFuture -> shardFuture
					.thenAcceptAsync(this::onShardLogin)
					.exceptionally(ExceptionLogger.get()));
		}

	}

	/**
	 * @param cacheSize キャッシュサイズ
	 */
	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * @param cacheStorageTimeInSeconds キャッシュ保持期間（秒数）
	 */
	public void setCacheStorageTimeInSeconds(int cacheStorageTimeInSeconds) {
		this.cacheStorageTimeInSeconds = cacheStorageTimeInSeconds;
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

	private void onShardLogin(DiscordApi api) {


		api.requestApplicationInfo().thenAccept(info -> {
			logger.info("Botを起動中... bot名:" + info.getName() + " shard:" + api.getCurrentShard());
		});

		api.setMessageCacheSize(cacheSize, cacheStorageTimeInSeconds);

		if (isMessageIntent) {
			api.addMessageCreateListener(event -> {
				DiscordMessageData dmd = new DiscordMessageData(event);

				try {
					for (IDiscordCommand command : commandList) {
						if (command.judgeExecute(dmd)) {

							if (command.checkInput(dmd)) {
								try {
									command.execute(dmd);
								} catch (Throwable e) {
									handleError(dmd, e);
								}
							} else {
								command.warning(dmd);
							}
							break;
						}
					}

				} catch (Throwable e) {
					handleError(dmd, e);
				}

			});
		}

		api.addServerMemberJoinListener(event -> {
			triggerEvent(new DiscordEventData(event, true));
		});
		api.addServerMemberLeaveListener(event -> {
			triggerEvent(new DiscordEventData(event, false));
		});
		api.addServerChannelChangeOverwrittenPermissionsListener(event -> {
			triggerEvent(new DiscordEventData(event, false));
		});
		api.addUserRoleAddListener(event -> {
			triggerEvent(new DiscordEventData(event, true));
		});
		api.addUserRoleRemoveListener(event -> {
			triggerEvent(new DiscordEventData(event, false));
		});

		api.addServerJoinListener(event -> event.getServer().getSystemChannel()
				.ifPresent(channel -> channel.sendMessage(joinMessage)));

		Set<SlashCommandBuilder> builderList = new HashSet<>();
	    for (IDiscordCommand command : commandList) {
			if (command instanceof IDiscordSlashCommand) {
				IDiscordSlashCommand slashCommand = (IDiscordSlashCommand)command;
				builderList.add(slashCommand.entryCommand());
			}
		}

	    try {
		    api.bulkOverwriteGlobalApplicationCommands(builderList).join();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }

		api.addSlashCommandCreateListener(event -> {
		    // コマンドの判断と実行を行う。
		    for (IDiscordCommand command : commandList) {
				if (command instanceof IDiscordSlashCommand) {
					IDiscordSlashCommand slashCommand = (IDiscordSlashCommand)command;
					DiscordMessageData dmd = new DiscordMessageData(event);
					if (event.getSlashCommandInteraction().getCommandName().
							equals((slashCommand.getCommandName()))) {
						try {
							slashCommand.executeSlashCommand(dmd);
						} catch (Throwable e) {
							handleError(dmd, e);
						}
					}

				}
			}
		});


		api.requestApplicationInfo().thenAccept(info -> {
			logger.info("Botの起動完了. bot名:" + info.getName() + " shard:" + api.getCurrentShard());
		});

	}

	private void triggerEvent(DiscordEventData ded) {
		try {
			for (IDiscordEvent event : eventList) {
				event.execute(ded);
			}
		} catch (Exception e) {
			handleError(ded, e);
		}
	}

	private void handleError(DiscordEventData ded, Throwable e) {
		logger.error("error event:" + ded.getGuild().getIdAsString() + ":" + ded.getGuild().getName(), e);
	}

	private void handleError(DiscordMessageData dmd, Throwable e) {
		logger.error("error command:" + dmd.getText(), e);
		dmd.getChannel().sendMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
	}

}
