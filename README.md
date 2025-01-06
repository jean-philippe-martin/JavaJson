# Java Json

A simple folding JSON viewer for the terminal, written in Java.

This isn't complete yet, but the basic functionality is there.

Available features:

- parse and display JSON files
- [folding](doc/features.md#folding)
- [pinning](doc/features.md#pinning) (pinned rows will be visible even within a folded region)
- [multicursor](doc/features.md#multicursor)
- [time annotation](doc/features.md#annotations) (convert from large number of seconds to hours or days)
- [color preview](doc/features.md#annotations) (256 colors, if the terminal supports it)
- scrolling
- [text search](doc/features.md#search)
- [sorting](doc/features.md#sorting)
- help screen showing the supported keys (press 'h' or '?')
- shift-Z to undo union or sort

Missing features:
- unicode support
- JSONL files
- color choice that works on light-colored terminals

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
java -jar target/JavaJson-0.4-SNAPSHOT-jar-with-dependencies.jar testdata/list.json
```

(Change the input file as desired)

Use the up and down arrows to navigate. h for help, q to quit.

## Intro to each feature

See the "[features](doc/features.md)" page.