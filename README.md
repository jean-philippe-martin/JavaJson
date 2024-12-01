# Java Json

A simple folding JSON viewer for the terminal, written in Java.

This isn't complete yet (most notably, scrolling is missing),
but the basic functionality is there.

Available features:

- parse and display JSON files
- folding
- pinning (pinned rows will be visible even within a folded region)
- multicursor
- time annotation (convert from large number of seconds to hours or days)
- color preview (256 colors, if the terminal supports it)
- scrolling

Missing features:

- unicode support
- help screen showing the supported keys
- text search
- JSONL files

## Testing

```
mvn test
```

## Building

```
mvn package
```

## Running

```
java -jar target/JavaJson-1.0-SNAPSHOT-jar-with-dependencies.jar testdata/list.json
```

(Change the input file as desired)

Use the up and down arrows to navigate. ESC to quit.

