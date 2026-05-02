# Development-related notes

## Building from code

You need to have a Java development environment set up of course.

Before you can compile you also need to have the HJSON branch that exposes comments. It's at
https://github.com/jean-philippe-martin/hjson-java-with-comments/tree/expose_comments
Git clone it to a sibling folder to where you have javajson.

Then:

```
mvn package
```

This will create a file like `target/JavaJson-1.17-snapshot-jar-with-dependencies.jar`

You can then run it with `./jj.sh` or with

```
java -jar target/JavaJson-1.17-snapshot-jar-with-dependencies.jar somejsonfile.json
```

## Testing

```
mvn test
```

## Building (without tests)

```
mvn package -DskipTests
```
