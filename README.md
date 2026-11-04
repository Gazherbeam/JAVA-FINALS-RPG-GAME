# Greenwood — Pixel RPG Practice

A small top-down Java game made with Swing. Explore the training grounds, move in
four directions, and practice swing attacks against the wooden dummy. The game
uses the supplied player idle sprite and eight attack frames when the `Pixel Art`
folder is available.

## Controls

- Move: **WASD** or **Arrow keys**
- Swing: **E**, **Space**, or **J**
- Face the dummy, move close, then swing to lower its health. It resets after
	three hits.

## Run

Requires a JDK (Java 17 or newer). From this project folder, compile and run:

```powershell
javac RpgSwingGame.java
java RpgSwingGame
```

The game checks for assets in a `Pixel Art` folder beside the project, in the
current directory, or at `%USERPROFILE%\Downloads\Pixel Art`. To use another
location, set the `rpg.assets` Java property to that folder when launching. If
the images are not found, built-in pixel-art-style sprites keep the game
playable.