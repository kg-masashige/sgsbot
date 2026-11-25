package csmp.bot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import csmp.bot.command.bcdice.BcDiceSearchSystemNameCommand;
import csmp.bot.command.bcdice.PlotOpenCommand;
import csmp.bot.command.bcdice.TableSetupCommand;
import csmp.bot.command.schedule.ScheduleCreateCommand;
import csmp.bot.command.sgs.ScenarioSendSecretCommand;
import csmp.utl.DiscordUtil;

/**
 * コマンドの処理のうち、Discordに接続しなくても確認できる部分のテスト.
 */
class CommandLogicTest {

	@Test
	void 結果一覧の解析() {
		Map<String, String> map = TableSetupCommand.parseResults("1:共感\r\n2：友情\n\n説明だけの行\n:出目なし\n 3 : 愛情 : 強い \n");
		assertEquals(new LinkedHashMap<>(Map.of("1", "共感", "2", "友情", "3", "愛情 : 強い")), new LinkedHashMap<>(map));
		assertEquals(List.of("1", "2", "3"), List.copyOf(map.keySet()));
		assertTrue(TableSetupCommand.parseResults("区切りなし").isEmpty());
		assertTrue(TableSetupCommand.parseResults(null).isEmpty());
	}

	@Test
	void メッセージの分割() {
		assertEquals(List.of("abc"), DiscordUtil.splitMessage("abc"));
		assertEquals(List.of(""), DiscordUtil.splitMessage(null));

		String line = "あ".repeat(999);
		List<String> chunks = DiscordUtil.splitMessage(line + "\n" + line + "\n" + line);
		assertEquals(2, chunks.size());
		assertEquals(line + "\n" + line, chunks.get(0));
		assertEquals(line, chunks.get(1));

		List<String> noBreak = DiscordUtil.splitMessage("x".repeat(4500));
		assertEquals(List.of(2000, 2000, 500), noBreak.stream().map(String::length).toList());
	}

	@Test
	void 文字数の切り詰め() {
		assertEquals("abc", DiscordUtil.truncate("abc", 3));
		assertEquals("ab…", DiscordUtil.truncate("abcd", 3));
	}

	@Test
	void システム名の部分一致検索() {
		Map<String, String> names = new LinkedHashMap<>();
		names.put("ShinobiGami", "シノビガミ");
		names.put("SwordWorld2.5", "ソード・ワールド2.5");
		assertEquals(List.of("シノビガミ"), BcDiceSearchSystemNameCommand.search(names, "ビガミ"));
		assertEquals(List.of("シノビガミ"), BcDiceSearchSystemNameCommand.search(names, "ｓｈｉｎｏｂｉ"));
		assertEquals(List.of("ソード・ワールド2.5"), BcDiceSearchSystemNameCommand.search(names, "２．５"));
		assertTrue(BcDiceSearchSystemNameCommand.search(names, "クトゥルフ").isEmpty());
	}

	@Test
	void 秘密の検索() {
		Map<Object, Object> secretMap = new LinkedHashMap<>();
		secretMap.put("pc", List.of(Map.of("name", "1", "secret", "PC1の秘密")));
		secretMap.put("npc", List.of(Map.of("name", "黒幕", "secret", "黒幕の秘密")));

		assertEquals("■PC1の秘密：\r\nPC1の秘密", ScenarioSendSecretCommand.findSecretMessage(secretMap, "pc1"));
		assertEquals("■黒幕の秘密：\r\n黒幕の秘密", ScenarioSendSecretCommand.findSecretMessage(secretMap, "黒幕"));
		assertEquals("■黒幕の秘密：\r\n黒幕の秘密", ScenarioSendSecretCommand.findSecretMessage(secretMap, "NPC黒幕"));
		assertNull(ScenarioSendSecretCommand.findSecretMessage(secretMap, "PC2"));
	}

	@Test
	void デイコードURLからキーを取り出す() {
		assertEquals("abc", ScheduleCreateCommand.getLinkKey("https://example.com/schedule/edit?key=abc"));
		assertEquals("abc", ScheduleCreateCommand.getLinkKey("https://example.com/schedule/edit?key=abc&x=1"));
		assertNull(ScheduleCreateCommand.getLinkKey("https://example.com/"));
	}

	@Test
	void プロットは公開すると消える() {
		long channelId = 100L;
		PlotOpenCommand.setPlot(channelId, 1L, "Aさん", "3");
		PlotOpenCommand.setPlot(channelId, 2L, "Bさん", "5");
		PlotOpenCommand.setPlot(channelId, 1L, "Aさん", "4");
		PlotOpenCommand.setPlot(200L, 3L, "Cさん", "1");

		assertEquals("■プロット\nAさん:4\nBさん:5", PlotOpenCommand.takePlots(channelId));
		assertNull(PlotOpenCommand.takePlots(channelId));
		assertEquals("■プロット\nCさん:1", PlotOpenCommand.takePlots(200L));
	}

}
