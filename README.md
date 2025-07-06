# Java Json

An open source interactive JSON viewer for the terminal. Includes useful features like folding, sort,
multiple cursors, groupby, JSONL support, and more. Watch the demo below for a taste.

![demo](doc/demo.gif)

Available features:

- parse and display JSON files or JSONL files
- [folding](doc/features.md#folding)
- [pinning](doc/features.md#pinning) (pinned rows will be visible even within a folded region)
- [multicursor](doc/features.md#multicursor)
- [time annotation](doc/features.md#annotations) (convert from large number of seconds to hours or days)
- [color preview](doc/features.md#annotations) (256 colors, if the terminal supports it)
- scrolling
- [text search](doc/features.md#search)
- [sorting](doc/features.md#sorting)
- [groupby](doc/features.md#groupby)
- help screen showing the supported keys (press 'h' or '?')
- shift-Z to undo union, sort, or aggregation
- support for Unicode and CJK scripts
- light or dark color options ("themes")

Missing features:
- support of right-to-left scripts

## Installation

To install, grab both the `jar` and the `jj` files from the latest release at this link:
[https://github.com/jean-philippe-martin/JavaJson/releases/latest](https://github.com/jean-philippe-martin/JavaJson/releases/latest)

You can then run the program with `./jj <your JSON file>`.
I recommend putting both files in your PATH.

You can download example JSON files from 
[https://github.com/jean-philippe-martin/JavaJson/tree/main/testdata](https://github.com/jean-philippe-martin/JavaJson/tree/main/testdata)

The program will work even with old versions of Java. If you don't already have Java on your computer,
I recommend downloading a recent version of OpenJDK.

On MacOS, the command is:

```
brew install openjdk
```

You can also [download it directly online](https://jdk.java.net/24/).

## Getting started

Starting JavaJson with a demo file will look something like this:

```
./jj testdata/demo.json	
```

Then you can use the arrow keys to navigate, <kbd>H</kbd> for help, 
and <kbd>M</kbd> to open the main menu.

The file can be in `JSON` or `JSONL` format (JSONL = each individual line is valid JSON).

You can add `--theme light` or `--theme dark` to choose colors that fit with a light or dark background in
your terminal. Or even `--theme bw` for simple black-on-white text.

## Documentation

For more information, read the documentation on [this page](doc/features.md).
