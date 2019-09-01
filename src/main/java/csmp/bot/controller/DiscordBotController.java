package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

public class DiscordBotController extends Thread {

	/**
	 * コマンドリスト.
	 */
	private List<DiscordCommandBase> commandList;

	/**
	 * discordクライアントインスタンス.
	 */
	private DiscordClient client;

	/**
	 * トークン.
	 */
	private String token;


	/**
	 * コンストラクタ.
	 * @param classList コマンド配列
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<Class<? extends DiscordCommandBase>> classList, String token) {
		this.token = token;
		commandList = new ArrayList<>();
		for (Class<? extends DiscordCommandBase> clazz : classList) {
			DiscordCommandBase command;
			try {
				command = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			commandList.add(command);
		}
	}

	/**
	 * 実行メソッド.
	 */
	@Override
	public void run() {
		System.out.println("Botを起動中...");

		client = new DiscordClientBuilder(token).build();

		client.getEventDispatcher().on(ReadyEvent.class)
		.subscribe(ready -> {
			System.out.println("Logged in as " + ready.getSelf().getUsername());
		});

		client.getEventDispatcher().on(MessageCreateEvent.class)
		.subscribe(event -> {
			Message message = event.getMessage();

			DiscordMessageData dmd = new DiscordMessageData(message);
			try {
				for(DiscordCommandBase command : commandList) {
					if (command.judgeExecute(dmd)) {
						if (command.checkInput(dmd)) {
							command.execute(dmd);
						} else {
							command.help(dmd);
						}
						break;
					}
				}

			} catch (Throwable e) {
				System.out.println("error command:" + dmd.getText());
				e.printStackTrace();
				message.getChannel().block().createMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
			}

		 });

		client.login().block();


	}

}
