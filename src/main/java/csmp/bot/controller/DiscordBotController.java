package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.model.DiscordMessageData;

public class DiscordBotController {

	/**
	 * コマンドリスト.
	 */
	private List<IDiscordCommand> commandList;

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
	 * @param classList コマンド配列
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<Class<? extends IDiscordCommand>> classList, String token, int totalShards) {
		this.token = token;
		this.totalShards = totalShards;
		commandList = new ArrayList<>();
		List<IDiscordCommand> commandListForHelp = new ArrayList<>();
		for (Class<? extends IDiscordCommand> clazz : classList) {
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
	}

	public DiscordBotController(List<Class<? extends IDiscordCommand>> classList, String token) {
		this(classList, token, 1);
	}

	/**
	 * 実行メソッド.
	 */
	public void execute(String joinMessage) {
		this.joinMessage = joinMessage;
		new DiscordApiBuilder().setToken(token)
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

		api.addServerJoinListener(event -> event.getServer().getSystemChannel()
				.ifPresent(channel -> channel.sendMessage(joinMessage)));

		System.out.println("Botの起動完了. shard:" + api.getCurrentShard());

	}

	private void handleError(DiscordMessageData dmd, Throwable e) {
		System.out.println("error command:" + dmd.getText());
		e.printStackTrace();
		dmd.getChannel().sendMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
	}

}
