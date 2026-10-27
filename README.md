# Item equals Border

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## Building

Build the Minecraft 26.2 version with its matching BetterBorderLib and Fabric API:

```powershell
.\gradlew.bat build "-Pminecraft_version=26.2" "-Ploader_version=0.19.5" "-Pfabric_api_version=0.156.0+26.2"
```

Build the Minecraft 26.3 version using the default properties:

```powershell
.\gradlew.bat build
```

The resulting jars have `mc26.2` or `mc26.3` in their filenames and are saved in `build/libs`.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
