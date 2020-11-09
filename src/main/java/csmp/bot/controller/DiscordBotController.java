package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.util.logging.ExceptionLogger;

import csmp.bot.command.IDiscordCommand;
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
	private int totalShards = 1;

	/**
	 * ログインメッセージ
	 */
	private String joinMessage = "";

	/**
	 * コンストラクタ.
	 * @param messageTriggerList コマンド配列
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<Class<? extends IDiscordCommand>> messageTriggerList, List<Class<? extends IDiscordEvent>> eventTriggerList, String token, int totalShards) {
		this.token = token;
		this.totalShards = totalShards;
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

	public DiscordBotController(List<Class<? extends IDiscordCommand>> messageTriggerList, List<Class<? extends IDiscordEvent>> eventTriggerList, String token) {
		this(messageTriggerList, eventTriggerList, token, 1);
	}

	/**
	 * 実行メソッド.
	 */
	public void execute(String joinMessage) {
		this.joinMessage = joinMessage;

		new DiscordApiBuilder().setToken(token)
				.setAllIntentsExcept(Intent.GUILD_PRESENCES)
				.setTotalShards(totalShards)
				.loginAllShards()
	            .forEach(shardFuture -> shardFuture
	                    .thenAcceptAsync(this::onShardLogin)
	                    .exceptionally(ExceptionLogger.get())
	                );
	}

	private void onShardLogin(DiscordApi api) {
		System.out.println("Botを起動中... shard:" + api.getCurrentShard());
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

		api.addServerMemberJoinListener(event -> {
			triggerEvent(new DiscordEventData(event));
		});
		api.addServerMemberLeaveListener(event -> {
			triggerEvent(new DiscordEventData(event));
		});
		api.addServerChannelChangeOverwrittenPermissionsListener(event -> {
			triggerEvent(new DiscordEventData(event));
		});
		api.addUserRoleAddListener(event -> {
			triggerEvent(new DiscordEventData(event));
		});
		api.addUserRoleRemoveListener(event -> {
			triggerEvent(new DiscordEventData(event));
		});

		api.addServerJoinListener(event -> event.getServer().getSystemChannel()
				.ifPresent(channel -> channel.sendMessage(joinMessage)));

		System.out.println("Botの起動完了. shard:" + api.getCurrentShard());

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
		System.out.println("error event:" + ded.getGuild().getIdAsString() + ":" + ded.getGuild().getName());
		e.printStackTrace();
	}

	private void handleError(DiscordMessageData dmd, Throwable e) {
		System.out.println("error command:" + dmd.getText());
		e.printStackTrace();
		dmd.getChannel().sendMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
	}

}
