# Java Json

A simple folding JSON viewer for the terminal, written in Java.

The basic functionality is there, it should be usable. There may still be some bugs though.

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

## Testing

```
mvn test
```

## Building (without tests)

```
mvn package -DskipTests
```

## Running

```
java -jar target/JavaJson-1.11-jar-with-dependencies.jar testdata/list.json
```

(If you're using a release JAR instead of building your own then adjust the jar's file name
as needed.)

(Change the input file as desired)

The file can be in `JSON` or `JSONL` format (JSONL = each individual line is valid JSON). 

You can add `--theme light` or `--theme dark` to choose colors that fit with a light or dark background in your
terminal. Or even `--theme bw` for simple black-on-white text.

Use the up and down arrows to navigate. h for help, q to quit.

## Intro to each feature

See the "[features](doc/features.md)" page.