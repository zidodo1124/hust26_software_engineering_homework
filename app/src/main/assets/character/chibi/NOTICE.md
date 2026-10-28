# Optional chibi whale-girl artwork

HUSTer's DDL Saver retains the classic illustrations and adds an optional chibi
appearance named **蓝色大肥鱼**. The original atlas is bundled byte-for-byte;
the application displays four selected, text-free regions without modifying,
recompressing or exporting derivative image files.

## Exact source and attribution

- **f0909172434**, atlas author/publisher:
  [dsh-deepseek-girl-pet](https://github.com/f0909172434/dsh-deepseek-girl-pet),
  commit `3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14`, `assets/spritesheet.webp`.
- Its per-asset source record points to the same author's
  [deepseek-girl-codex-pet](https://github.com/f0909172434/deepseek-girl-codex-pet),
  commit `2572709632b0e81401b9e92994f2c3786c186397`, `pet/spritesheet.webp`.
- The publisher's `atlas-source.json` explicitly marks this shared atlas as MIT
  and records its SHA-256. The copied source record and both complete MIT notices
  are included here as `upstream-atlas-source.json`, `upstream-LICENSE.txt`, and
  `origin-LICENSE.txt`.
- [JAdpp's per-file notice](https://github.com/JAdpp/dsh-whale-galgame/blob/046b848f244450e6e636335766276bf3343fe84b/NOTICE.md)
  independently lists the identical atlas and the same hash as a separately
  MIT-licensed asset. No JAdpp code or other pet resources are used here.

## Rights remain separate

Copyright (c) 2026 f0909172434. The MIT grant applies to the contributions the
publisher has rights to license; it is not a new license to underlying character
designs, third-party marks or all whale-girl artwork on the Internet.

The original whale-girl attribution remains **上善 / 上善无形** (original
character) and **ZipZipPipe** (maid redesign incorporating DeepSeek elements).
Their recorded CC BY-NC-SA 4.0 conditions and attribution are retained in
[`../NOTICE.md`](../NOTICE.md) and the accompanying CC license. This noncommercial
course project does not use the atlas's MIT notice to override those underlying
conditions. The separate classic standing-art chain through Small-tailqwq and
JAdpp is also retained. Neither appearance is official artwork, an app logo,
or an endorsement by DeepSeek or any listed creator.

## Rendering map

The WebP has an alpha channel and measures 1536 × 2288 pixels: eight columns,
eleven rows, with 192 × 208 pixels per cell. Coordinates below are zero-based.

| App expression | Row | Column | Source rectangle (left, top, width, height) |
| --- | ---: | ---: | --- |
| Normal | 0 | 6 | 1152, 0, 192, 208 |
| Happy / completed | 4 | 1 | 192, 832, 192, 208 |
| Serious / soon or overdue | 8 | 1 | 192, 1664, 192, 208 |
| Focused / danger | 7 | 1 | 192, 1456, 192, 208 |

These regions were visually checked: they contain no speech bubble or sign text.
The serious pose holds a hand near the face; the danger pose stands with a
concentrated frown. The source author describes these rows as review and work
animations, respectively; mapping them to DDL states is this application's
presentation choice, not a claim that the author created a DDL-specific set.
State names and dialogue remain visible so meaning does not depend on a face.
This application uses static frames, not the source project's animation or
pointer tracking. `sources.json` records hashes, original bytes and this map.
