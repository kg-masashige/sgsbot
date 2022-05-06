# シノビガミセッションサポートbot

[忍術バトルRPGシノビガミ -忍神-](http://www.bouken.jp/pd/sg/)をDiscordを使って遊びやすくするためのbotです。<br>
シノビガミ以外のセッションでもご利用いただけます。<br>
<br>
また、以下で紹介する機能の他に、Discordと連携する日程調整サービス「[デイコード](https://character-sheets.appspot.com/schedule/)」の機能もご利用いただけます。


## 使用方法
１．[こちらのリンク](https://discordapp.com/oauth2/authorize?client_id=611880245707931648&permissions=8&scope=bot%20applications.commands)から自分が管理しているサーバにbotを招待する。

２．/sgshelp と発言して使用方法を確認する。



使用できる機能は以下の通りです。

### 秘密配付機能
[シノビガミシナリオ登録サイト](https://character-sheets.appspot.com/sgScenario/)と連動し、プレイヤーに秘密を配ることができます。

[サンプルシナリオ](https://character-sheets.appspot.com/sgScenario/detail?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM)を取り込む場合、
botを参加させたサーバで以下の通り発言してください。

~~~
/sgss https://character-sheets.appspot.com/sgScenario/detail?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM
~~~

すると、PC1～PC4までのチャンネルが作られ、それぞれの導入、使命、秘密が貼られます。<br>
チャンネルはそれぞれのPC番号に沿ったDiscordのロールが割り振られるため、ロールを持った人とサーバ管理者しか観ることができません。<br>
サーバ管理者は各プレイヤーにPC番号のロールを割り振ってください。（※）<br>

※ロールを割り振るには、Discordのサーバ参加者を右クリックして以下の通り。
![role_sample](https://user-images.githubusercontent.com/54463631/84027713-615a0980-a9ca-11ea-90c5-b8e7f164dfde.png)


### セッション予定管理機能
このbotは、「[デイコード](https://character-sheets.appspot.com/schedule/)」が内包されております。デイコードのコマンドをご利用ください。



### ダイスボット機能
各種オンラインセッションツールでで採用されているBCDice 3の機能を利用してダイスを振ることができます。

セッション開始時や再開時に

~~~
/bcdiceset シノビガミ
~~~

と発言してください。
すると指定したシステムのダイスボットを振ることができます。

システムを設定したあとは、

~~~
/roll 2D6
~~~

などとコマンドを半角で入力することでダイスを振ることができます。



#### キャラクターシート倉庫との連携
[シノビガミ用キャラクターシート](https://character-sheets.appspot.com/shinobigami/)と連携してダイスを振ることができます。

１．キャラクターシートのDiscord連携チェックに✓を入れてキャラクターシートを登録してください。

２．登録したキャラクターシートのURL（[例](https://character-sheets.appspot.com/shinobigami/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY-eKkrwEM)）をコピーし、Discord上で「/link <コピーしたURL>」と発言してください。

~~~
/link https://character-sheets.appspot.com/shinobigami/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY-eKkrwEM
~~~

３．キャラクターシートのDRボタンで判定を行うことができます。


### プロット機能
シノビガミのルールで使用するプロット機能を使用することができます。

１．導入したbotのいるチャンネルで、「/plot <プロット値>」と発言してプロットしてください。<br>
（例　<プロット値>は「プロット：6」、「影分身で2と5」など）

２．全員のプロットが揃ったら、「/openplot」と発言してプロットを公開してください。


### 利用しているサービス
[BCDice](https://github.com/bcdice/BCDice)

日本のデファクトスタンダードなオンセツール用ダイスエンジン。[修正BSDライセンス](https://github.com/bcdice/BCDice/blob/master/LICENSE)でライセンスされています。



[BCDice-API](https://github.com/ysakasin/bcdice-api)

BCDiceが振れるWebAPI。[MITライセンス](https://github.com/ysakasin/bcdice-api/blob/master/LICENSE)でライセンスされています。



[とあるTRPG鯖](https://bcdice.onlinesession.app/)

色々なオープンソースのTRPG関連ツールを無償で提供しているサーバー。設置されているBCDice-APIを使用。


[キャラクターシート倉庫](https://character-sheets.appspot.com/)

キャラクターシートを管理するためのサイト。シノビガミのキャラシ、シナリオシートの管理、スケジュール管理の連携先として使用。


### 使用しているライブラリ
[javacord](https://github.com/Javacord/Javacord)

Discordとの通信で使用。[Apache License 2.0](https://github.com/Javacord/Javacord/blob/master/LICENSE)でライセンスされています。


[OkHttp](https://square.github.io/okhttp/)

Dicebot、キャラクターシートへの連携で使用。[Apache License 2.0](https://square.github.io/okhttp/#license)でライセンスされています。


[JSONIC](https://github.com/hidekatsu-izuno/jsonic)

JSONの解析、作成に使用。[Apache License 2.0](https://github.com/hidekatsu-izuno/jsonic/blob/master/LICENSE)でライセンスされています。


### 参考にしたサービス
[BCDice bot for Discord](https://github.com/Shunshun94/discord-bcdicebot)

DiscordでBCDiceを振ることに特化したbot。BCDiceとの連携処理、Javacordの使用方法などで参考にさせていただきました。



### 連絡先
bot作成者への連絡は[Twitter](https://twitter.com/kg_masashige/)でお願いします。<br>
不具合などご報告いただけるとたすかります。

### 支援
支援は[pixivFANBOX](https://kg-masashige.fanbox.cc/)へいただけると助かります。
[キャラクターシート倉庫](https://character-sheets.appspot.com/)トップからのAmazonギフト券でも受け付けております。
