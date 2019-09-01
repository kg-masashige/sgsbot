package csmp.bot.command;

import csmp.bot.model.DiscordMessageData;

public abstract class DiscordCommandBase {

	public abstract boolean judgeExecute(DiscordMessageData dmd);

	public abstract boolean checkInput(DiscordMessageData dmd);

	public abstract void execute(DiscordMessageData dmd);

	public void help(DiscordMessageData dmd) {
		// デフォルトヘルプメッセージ。
		String helpText = "■コマンド一覧（<>はつけずに実行してね）：\r\n" +
				"・シナリオセットアップコマンド：\r\n" +
				"　/sgss <シナリオシートURL>：\r\n　　　シナリオシートの情報を読み込んでチャンネル、権限を設定する。\r\n" +
				"　/sgsread <シナリオシートURL>：\r\n　　　シナリオシートの情報の読み込みだけ行う。\r\n" +
				"　/sgsclear：\r\n　　　シナリオシートの情報、チャンネル、権限を削除する。\r\n" +
				"　/sgssend <秘密名> <PC名>：\r\n　　　PC名のチャンネルに指定された秘密を張り付ける。\r\n" +
				"\r\n" +
				"・スケジュールコマンド：\r\n" +
				"　/scheadd <日付> <リマインドメッセージ（オプション）>：\r\n　　　セッション予定日になったらリマインドメッセージを飛ばす。\r\n" +
				"　　　日付を指定する時はカンマ区切りで複数指定可能。例：/scheadd 9/1,9/2\r\n" +
				"　/schedel <日付> ：\r\n　　　指定したセッション予定日を削除する。\r\n" +
				"　　　日付を指定する時はカンマ区切りで複数指定可能。allを指定すれば全て削除。例：/schedel 9/1,9/2\r\n" +
				"　/scheshow ：\r\n　　　登録済のセッション予定日を表示する。\r\n";

		dmd.getChannel().createMessage(helpText).block();	}

}
