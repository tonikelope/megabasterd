<h1>MegaBasterd</h1>

[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)](https://GitHub.com/Naereen/StrapDown.js/graphs/commit-activity) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<p align="center"><i>"If it compiles, it's good; if it boots up, it's perfect." (Linus Torvalds)</i></p>
<p align="center"><a href="https://github.com/tonikelope/megabasterd/releases/latest" target="_blank"><img src="https://raw.githubusercontent.com/tonikelope/megabasterd/master/megabasterd-desktop/src/main/resources/images/mbasterd_logo_git.png"></a></p>
<h2 align="center"><a href="https://github.com/tonikelope/megabasterd/releases/latest" target="_blank"><b>Download latest build</b></a></h2>
<h3 align="center"><i>Note: MegaBasterd jar version requires <a href="https://adoptium.net/es/temurin/releases/?version=11" target="_blank">Java 8 or later</a>.</i></h3>
<p align="center"><a href="https://github.com/tonikelope/megabasterd/releases/latest" target="_blank"><img src="https://raw.githubusercontent.com/tonikelope/megabasterd/master/megabasterd-desktop/src/main/resources/images/linux-mac-windows.png"></a></p>
<p align="center"><a href="https://github.com/tonikelope/megabasterd/issues/397"><b>Would you like to help by translating MegaBasterd into your favorite language?</b></a></p>


![Screnshot](/megabasterd-desktop/src/main/resources/images/mbasterd_screen.png)




<p align="center"><a href="https://youtu.be/5TkBXT7osQI"><b>MegaBasterd DEMO</b></a></p>

<p align="center"><img src="https://raw.githubusercontent.com/tonikelope/megabasterd/master/coffee.png"><br><img src="https://raw.githubusercontent.com/tonikelope/megabasterd/master/megabasterd-desktop/src/main/resources/images/ethereum_toni.png"></p>

<p align="center"><a href="https://github.com/tonikelope/megabasterd/issues/385#issuecomment-1019215670">BONUS: Why the f*ck has MegaBasterd stopped downloading?</a></p>

<p align="center"><b>IMPORTANT:</b> You are not authorized to use MegaBasterd in any way that violates <a href="https://mega.io/es/terms"><b>MEGA's terms of use</b></a>.</p>

## Building from source

MegaBasterd is built as a Maven multi-module project:

- `megabasterd-core` builds the internal core library jar.
- `megabasterd-desktop` builds the current Swing desktop application.

Build everything from a clean checkout with:

```sh
mvn clean package
```

The runnable desktop artifact is:

```text
megabasterd-desktop/target/MegaBasterd-8.46-jar-with-dependencies.jar
```

Run it with:

```sh
java -jar megabasterd-desktop/target/MegaBasterd-8.46-jar-with-dependencies.jar
```

For a faster desktop-only rebuild after the core dependency has already been built locally:

```sh
mvn -pl megabasterd-desktop -am package
```

Useful local verification commands:

```sh
python3 scripts/verify_i18n_refs.py
python3 scripts/verify_i18n_forms.py
mvn -q -pl megabasterd-desktop -am compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
javac -cp "megabasterd-desktop/target/classes:megabasterd-core/target/classes:$(cat megabasterd-desktop/target/classpath.txt)" -d megabasterd-desktop/target/classes scripts/I18nSmoke.java scripts/SmartProxyParserSmoke.java
java -cp "megabasterd-desktop/target/classes:megabasterd-core/target/classes:$(cat megabasterd-desktop/target/classpath.txt)" com.tonikelope.megabasterd.I18nSmoke
java -cp "megabasterd-desktop/target/classes:megabasterd-core/target/classes:$(cat megabasterd-desktop/target/classpath.txt)" com.tonikelope.megabasterd.SmartProxyParserSmoke
```
