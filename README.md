# DataToMC — Data Encoder/Decoder for Minecraft

This thing takes any file, decodes it into raw bits and bytes --> base ~900, then using an array of elementss 0-base-1 to represent the encoded files and matches each one to an id in a json with the same # of elements as the base then uses Querz with a custom section encoder to make Querz work in the latest versions which allows it to place the blocks in an MCA file. the same process backwards decodes it!
WARNING!! it may take a VERY long time for larger files (a test i did took 30s for 305kb, and this can support up to 300x that, so if you plan to store a large image, be prepared for a wait, however this does come with the upside of enough storage to even store small videos.)
ALSO WHEN IT IS LOADED, DO NOT TOUCH ANYTHING OR DECRYPTING IT MIGHT BREAK!!!!
I removed gravity blocks because any block update can ruin it, and you might see things like floating torches, that is normal I also tried to remove as many blocks that would ruin it as possible, but I'm not sure if I removed them all..
ALSO some blocks like banners or beds (the 2 main ones) will appear to be blank spaces. this is because since their shape is unique, they're textures do not load, making it look like air, but middle clicking or breaking them does tell you what they are.
---



### Capacity

Each region file (32×32 chunks, Y −64 to 319) can hold up to:

```
512 × 512 × 384 = 100,663,296 blocks
```

With a palette of 949 blocks, each block encodes roughly  9.9 bits, giving a  maximum file size of around **118 MB** per region file.

---

## Block Palette

The block palette is defined in `app/src/main/resources/ids.json`. It contains 949 Minecraft block IDs. Index 0 must be `minecraft:air` as it is used as the end-of-data sentinel. The palette size directly determines the encoding base — adding or removing blocks changes `BASE` and breaks compatibility with previously encoded files.

---

## Requirements

- Java 21
- Gradle (wrapper included — no separate install needed)
- Minecraft Java Edition 1.21.11 (for loading the output `.mca`)

---

## Building

### Option 1 — jar (recommended for non devs)


To run it, double-click `DataToMC.jar`. If double-clicking does not work (common on Linux), run it from the terminal:

```bash
java -jar DataToMC.jar
```

**On Windows**, if `.jar` files are not associated with Java, right-click the jar → Open with → Java Platform SE Binary. To fix this permanently, reinstall Java and make sure the option to associate `.jar` files is checked, or set the association manually in Default Apps settings.

**On macOS**, if Gatekeeper blocks it, right-click → Open → Open anyway.

### Option 2 — Run via Gradle (development)

```bash
./gradlew run
```

---

## Usage

### Encoding a file

Open the jar file with java, select the encode button, choose a file, and it will create an encoded mca in your downloads. put this in minecrafthome/saves/world you want to use it with/region/ and replace the current mca
###
Decoding
open and mca file after opening the jar, select decode, wait, and look in downloads folder for your decoded MCA

> **Warning:** placing `r.0.0.mca` in a world overwrites the 32×32 chunk area starting at world origin. Use a dedicated world or a region that does not contain anything important.


## Project Structure

```
DatatoMinecraftEncoder/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/org/example/Main.java
│       └── resources/
│           └── ids.json
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
└── README.md
```

---

## Dependencies

| Library | Purpose |
|---------|---------|
| `com.github.Querz:NBT:6.1` | NBT tag serialization/deserialization |
| `com.googlecode.json-simple:json-simple:1.1.1` | Parsing `ids.json` |
| Guava | General utilities |

All dependencies are bundled into the fat jar automatically by Gradle.

---

## Compatibility

Use version **1.21.11**, theoretically compatible with all OSs but only tested on windows
---
## License

MIT License

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
