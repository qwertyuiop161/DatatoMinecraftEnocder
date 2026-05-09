# DataToMC — Data Encoder/Decoder for Minecraft

DataToMC encodes any file into a Minecraft 1.21.11 region file (`.mca`) by representing the file's bytes as a sequence of Minecraft blocks, then decodes it back to the original file. The encoded data is stored as block states across chunks in a valid region file that can be placed directly into any Minecraft world.
WARNING!! it may take a VERY long time for larger files (a test i did took 30s for 305kb, and this can support up to 300x that, so if you plan to store a large image, be prepared for a wait, however this does come with the upside of enough storage to even store small videos.)
ALSO WHEN IT IS LOADED, DO NOT TOUCH ANYTHING OR DECRYPTING IT MIGHT BREAK!!!!
I removed gravity blocks because any block update can ruin it, and you might see things like floating torches, that is normal I also tried to remove as many blocks that would ruin it as possible, but I'm not sure if I removed them all..
ALSO some blocks like banners or beds (the 2 main ones) will appear to be blank spaces. this is because since their shape is unique, they're textures do not load, making it look like air, but middle clicking or breaking them does tell you what they are.
---

## How It Works

### Encoding

1. The file's extension length and characters are stored as the first few block indices.
2. The file's byte length is stored as the next three indices (base-949 digits).
3. The file's raw bytes are converted to a big integer, then expressed in base 949 (the number of blocks in `ids.json`), producing a sequence of block indices.
4. The sequence is padded with index 0 (`minecraft:air`) until it fills the full region capacity.
5. Each index maps to a block name from `ids.json`, producing a list of block names.
6. The block names are written into chunks of `r.0.0.mca` in order: chunk Z outer, chunk X inner, Y top-down (319 → −64), X (0 → 15), Z (0 → 15).
7. Air blocks (index 0) are left as the natural empty state — no block is written for them.

### Decoding

1. The region file is read in the exact same traversal order.
2. Block names are mapped back to their indices using `ids.json`.
3. Reading stops the moment a `minecraft:air` block (index 0) is encountered — this marks the end of encoded data.
4. The index sequence is decoded: extension is read first, then byte length, then the big integer is reconstructed and converted back to the original bytes.
5. The file is written to Downloads with its original extension.

### Capacity

Each region file (32×32 chunks, Y −64 to 319) can hold up to:

```
512 × 512 × 384 = 100,663,296 blocks
```

With a palette of 949 blocks, each block encodes roughly log₂(949) ≈ 9.9 bits, giving a theoretical maximum file size of around **118 MB** per region file.

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

1. Double-click `DataToMC.jar` to launch the app.
2. Click **Select File to Encode**.
3. Choose any file in the file picker.
4. The status bar shows progress: Encoding → Writing MCA.
5. When done, `r.0.0.mca` is saved to your Downloads folder.
6. The temporary copy of the input file is automatically deleted from the working directory.

### Decoding a file

1. Double-click `DataToMC.jar` to launch the app.
2. Click **Select MCA to Decode**.
3. Choose an `r.0.0.mca` file produced by this tool.
4. The status bar shows progress: Reading MCA → Decoding.
5. When done, `output.<original extension>` is saved to your Downloads folder.
6. The temporary copy of the MCA file is automatically deleted from the working directory.

---

## Output Locations

| Operation | Output file | Location |
|-----------|-------------|----------|
| Encode | `r.0.0.mca` | `~/Downloads/` |
| Decode | `output.<ext>` | `~/Downloads/` |

If a `Downloads` folder does not exist (some Linux systems), output falls back to the home directory.

---

## Using the MCA in Minecraft

1. Create or open a Minecraft Java Edition 1.21.11 world.
2. Locate the world's region folder: `.minecraft/saves/<world name>/region/`.
3. Back up any existing `r.0.0.mca` if present.
4. Copy the generated `r.0.0.mca` into the region folder.
5. Load the world. Chunks 0,0 through 31,31 (block coordinates 0,−64 to 511,319) will contain the encoded data as blocks.

> **Warning:** placing `r.0.0.mca` in a world overwrites the 32×32 chunk area starting at world origin. Use a dedicated world or a region that does not contain anything important.

---

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

| OS | Supported | Notes |
|----|-----------|-------|
| Windows | ✅ | Double-click the jar to run |
| macOS | ✅ | May need to allow in Security & Privacy on first run |
| Linux | ✅ | Run with `java -jar DataToMC.jar` if double-click is unsupported |

The MCA output targets **Minecraft Java Edition 1.21.11** (DataVersion 4671). Loading it in other versions may trigger a world upgrade prompt or cause data loss.

---

## Limitations

- Maximum encodable file size is approximately **90 MB** (one full region).
- Only one file can be encoded per region. Multiple files require multiple regions (not currently supported).
- The output filename on decode is always `output.<ext>` — the original filename is not preserved, only the extension.
- Decoding requires the same `ids.json` that was used during encoding. If the palette changes, decoding will produce garbage output.

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