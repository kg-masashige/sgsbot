# シノビガミセッションサポートbot

[忍術バトルRPGシノビガミ -忍神-](http://www.bouken.jp/pd/sg/)をDiscordを使って遊びやすくするためのbotです。


## 使用方法
１．[こちらのリンク](https://discordapp.com/oauth2/authorize?client_id=611880245707931648&permissions=8&scope=bot)からbotを自分が管理しているサーバに招待する。

２．/sgshelp と発言して使用方法を確認する。



使用できる機能は以下の通りです。

### 秘密配付機能
[シノビガミシナリオ登録サイト](https://character-sheets.appspot.com/sgScenario/)と連動し、プレイヤーに秘密を配ることができます。

[サンプルシナリオ](https://character-sheets.appspot.com/sgScenario/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM)を取り込む場合、
botを参加させたサーバで以下の通り発言してください。

~~~
/sgss https://character-sheets.appspot.com/sgScenario/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM
~~~

すると、PC1～PC4までのチャンネルが作られ、それぞれの導入、使命、秘密が貼られます。<br>
チャンネルはそれぞれのPC番号に沿ったDiscordの役割が割り振られるため、役割を持った人とサーバ管理者しか観ることができません。<br>
サーバ管理者は各プレイヤーにPC番号の役割を割り振ってください。<br>

### セッション予定管理機能
セッションの次回予定日を登録し、リマインドすることができます。

botを参加させたサーバで以下の通り発言してください。

~~~
/scheadd 9/16,9/17
~~~

すると、<br>
ShinobigamiScenarioSetupBOT<br>
2019/09/16,2019/09/17を登録しました。<br>

と返事があります。

登録した日付がみたい場合は

~~~
/scheshow
~~~

と発言してください。

すると<br>
予定日は：<br>
2019/09/16(月)<br>
2019/09/17(火)<br>

と表示されます。

登録した日付の日本時間0時になると<br>
2019/09/16はセッション予定日です。<br>
<リマインドメッセージ>

と表示されます。

### 連絡先
bot作成者への連絡は[Twitter](https://twitter.com/kg_masashige/)でお願いします。

### 支援
支援は[pixivFANBOX](https://www.pixiv.net/fanbox/creator/7622752)へいただけると助かります。<br>
[キャラクターシート倉庫](https://character-sheets.appspot.com/)トップからのAmazonギフト券でも受け付けております。
