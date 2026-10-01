# JD-GUI

[![CI](https://github.com/mr-fixit-sj/jd-gui-pro/actions/workflows/ci.yml/badge.svg)](https://github.com/mr-fixit-sj/jd-gui-pro/actions/workflows/ci.yml)

JD-GUI, a standalone graphical utility that displays Java sources from CLASS files.

This is a fork of [java-decompiler/jd-gui](https://github.com/java-decompiler/jd-gui), updated to work
with class files compiled by recent JDKs (see [Changes in this fork](#changes-in-this-fork)).

![](src/website/img/jd-gui.png)

- JD-GUI source code (this fork): [https://github.com/mr-fixit-sj/jd-gui-pro](https://github.com/mr-fixit-sj/jd-gui-pro)
- Original JD-GUI source code: [https://github.com/java-decompiler/jd-gui](https://github.com/java-decompiler/jd-gui)
- Java Decompiler projects home page: [http://java-decompiler.github.io](http://java-decompiler.github.io)

## Description
JD-GUI is a standalone graphical utility that displays Java source codes of 
".class" files. You can browse the reconstructed source code with the JD-GUI
for instant access to methods and fields.

## How to build JD-GUI ?
Requirements:
- A JDK between 11 and 24 that includes the `jmods` directory (used by ProGuard). Built and tested
  by CI with JDK 11, 17 and 21. JDK 25+ is not supported yet by the Gradle version used (8.14).
- Network access to Maven Central, the Gradle Plugin Portal and [JitPack](https://jitpack.io)
  (JD-Core is no longer published on Maven Central and is built by JitPack from its GitHub repository).

```
> git clone https://github.com/mr-fixit-sj/jd-gui-pro.git
> cd jd-gui-pro
> ./gradlew build 
```
generate :
- _"build/libs/jd-gui-x.y.z.jar"_
- _"build/libs/jd-gui-x.y.z-min.jar"_
- _"build/distributions/jd-gui-windows-x.y.z.zip"_
- _"build/distributions/jd-gui-osx-x.y.z.tar"_

The resulting JD-GUI runs on Java 8+.

## Changes in this fork
- Search, "Open Type", "Open Type Hierarchy" and the class outline work with class files compiled
  for Java 14 and later (ASM upgraded from 7.1 to 9.10.1).
- String constant search also matches multi-line strings (text blocks).
- The build runs on current JDKs: Gradle 8.14 instead of 5.2.1, Maven Central and JitPack instead
  of the discontinued JCenter, and updated ProGuard and Launch4j plugins.
- The Linux `.deb` and `.rpm` packages are no longer built: on Linux, run the jar directly.

## Supported class file versions
- Browsing, search, "Open Type" and "Open Type Hierarchy" support class files up to Java 27.
  Newer class files are read on a best-effort basis.
- Decompilation is done by JD-Core 1.1.3, which predates Java 14. Classes compiled with newer
  versions are decompiled, but recent language features are not fully reconstructed: records
  appear as classes extending `java.lang.Record` (with `toString`/`equals`/`hashCode` shown as
  byte code), `sealed`/`permits` is omitted and methods using pattern matching `switch` are
  shown as byte code.

Every push to `master` and every pull request is built and tested by
[GitHub Actions](https://github.com/mr-fixit-sj/jd-gui-pro/actions/workflows/ci.yml); the jars and the
Windows and macOS distributions of each run can be downloaded from its "jd-gui" artifact.

## How to launch JD-GUI ?
- Double-click on _"jd-gui-x.y.z.jar"_
- Double-click on _"jd-gui.exe"_ application from Windows
- Double-click on _"JD-GUI"_ application from Mac OSX
- Execute _"java -jar jd-gui-x.y.z.jar"_ or _"java -classpath jd-gui-x.y.z.jar org.jd.gui.App"_

## How to use JD-GUI ?
- Open a file with menu "File > Open File..."
- Open recent files with menu "File > Recent Files"
- Drag and drop files from your file explorer

## How to extend JD-GUI ?
```
> ./gradlew idea 
```
generate Idea Intellij project
```
> ./gradlew eclipse
```
generate Eclipse project
```
> java -classpath jd-gui-x.y.z.jar;myextension1.jar;myextension2.jar org.jd.gui.App
```
launch JD-GUI with your extensions

## How to uninstall JD-GUI ?
- Java: Delete "jd-gui-x.y.z.jar" and "jd-gui.cfg".
- Mac OSX: Drag and drop "JD-GUI" application into the trash.
- Windows: Delete "jd-gui.exe" and "jd-gui.cfg".

## License
Released under the [GNU GPL v3](LICENSE).

## Donations
Donations go to Emmanuel Dupuy, the author of the original JD-GUI and JD-Core.

Did JD-GUI help you to solve a critical situation? Do you use JD-Eclipse daily? What about making a donation?

[![paypal](src/website/img/btn_donate_euro.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=C88ZMVZ78RF22) [![paypal](src/website/img/btn_donate_usd.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CRMXT4Y4QLQGU)
