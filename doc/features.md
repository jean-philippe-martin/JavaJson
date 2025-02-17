# Features

The program focuses on viewing JSON files and has the following features:

- [Folding](#folding)
- [Pinning](#pinning)
- [Multicursor](#multicursor)
- [Find](#search)
- [Sort](#sorting)
- [Sibling selection](#sibling-selection)
- [Annotations](#annotations)
- [Unique keys](#Unique-keys)
- [Groupby](#groupby)

## Basic navigation

You use `up arrow`, `down`, `page up`, `page down`, `home` and `end` to navigate through the document.
The view will scroll as needed.

You can press `?` to view a help screen with a list of keys and their functions.

You can press `q` to quit the program.

## Folding

You can fold sections of the document to better focus on what you are interested in. For example:

Start the program as shown below (you can skip the compilation step if you downloaded the jar):

```shell
mvn package -DskipTests
java -jar target/JavaJson-1.4-SNAPSHOT-jar-with-dependencies.jar testdata/hello.json
```

Here is what you'd see after pressing `arrow down` a few times to reach "count_to_five".
```
{
  "greeting": "hello"
  "who": "world"
>>"count_to_five": [ // 5 entries
    1
    2
    3
    4
    5
  ]
  "greek_letters": [ // 3 entries
    "alpha"
    "beta"
    "gamma"
  ]
```

Now, suppose `count_to_five` takes up too much of your screen and it's not important at the moment. You could *fold* it by pressing the `left arrow` key.
The screen would now look like this:

```
{
  "greeting": "hello"
  "who": "world"
>>"count_to_five": [ ... ] // 5 entries
  "greek_letters": [ // 3 entries
    "alpha"
    "beta"
    "gamma"
  ]
  "recording": {
    "duration_sec": // 1.47 h
                    5300
    "operator": "Joe"
    "vol_setting": 10.3
  }
```

As you can see, the ellipsis indicates that the list was hidden. This works on maps as well (the things with "{" that include named fields).

You can un-fold sections with the `right arrow` key.

Folding interacts with [pinning](#pinning) and [multicursors](#multicursor). Folding state isn't saved to disk (nothing is - this is a viewer only).

## Pinning

We can [fold](#folding) sections of the document we do not want to see, but what if we care about some small part of it? 

Pinning marks rows so they will be shown even when they are inside a folded region.

For example, let's open the car_maintenance test file (you can skip the build step if you already have the jar file).

```shell
mvn package -DskipTests
java -jar target/JavaJson-1.4-SNAPSHOT-jar-with-dependencies.jar testdata/car_maintenance.json
```
Here's what it looks like after we lower the cursor a few times with `arrow down`:

```
[ // 1 entry
  {
    "vehicle": [ // 2 entries
      {
>>      "make": "Subaru"
        "model": "Forester"
        "insurance": {
          "company": "Allyourmoney"
          "policy_num": "98765"
        }
```

Suppose we only cared about the make of the vehicles. To hide everything else, we first press `p` to pin this row, and then `left arrow` twice to fold.

The result should look like this:

```
[ // 1 entry
  {
    "vehicle": [ // 2 entries
>>    { ...
P       "make": "Subaru"
      }
      {
        "make": "Toyota"
        "model": "RAV4"
```

As you can see, the selected vehicle is now summarized as just its make.

Pressing `p` again on a pinned item will unpin it. This may cause it to be hidden from view if it's in a folded region.

The letter 'P' is shown on the left margin to indicate whether a row is pinned.

Pinning interacts with [folding](#folding) and [multicursors](#multicursor). Pinned state isn't saved to disk (nothing is - this is a viewer only).


## Multicursor

Multicursor is that idea that you can select multiple parts of the document at once and act on all of them simultaneously.

There are two main ways to get multiple cursors: [text search](#search) and [sibling selection](#sibling-selection).

Search is the easiest to explain, so let's start there.

Let's open the car_maintenance test file (you can skip the build step if you already have the jar file).

```shell
 mvn package -DskipTests
 java -jar target/JavaJson-1.4-SNAPSHOT-jar-with-dependencies.jar testdata/car_maintenance.json
```

Now press the `f` key to open the find dialog, type `make` and press the `enter` key.

The "make" field in every vehicle should be highlighted now, indicating you have multiple cursors: whatever you do will happen to all of them.

Only one row has the `>>` indicator: this is the main cursor. That's the one that will remain if you remove the multicursors with the `ESC` key.

You can press `n` and `N` to move from one secondary cursor to the next. This is especially useful if some of them are beyond the visible area of the screen.

If now you pin using the `p` key, you will notice that both of the "make" rows are marked with the "P" in the margin. If you then fold the "vehicle" section, the make of both vehicles will still show.

You should see this:

```
[ // 1 entry
  {
>>  "vehicle": [ ... // 2 entries
      { ...
P       "make": "Subaru"
      }
      { ...
P       "make": "Toyota"
      }
    ]
  }
]
```

## Search

Search will put a cursor at every row that matches your query. It will also move your primary cursor over to one of the matches, ensuring that only the matches are selected.

The search dialog follows the consistent UI style of this application, now's a good time to introduce it.

Here is what you will see when you press `f` to open the "find" dialog.

```
┏━━━━━━━━━━━━[ FIND ]━━━━━━━━━━━━┓
> █                              <
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
│ Whole │ Aa │ K+V │ // │ .* │   │
├───────┴────┴─────┴────┴────┴───┤
│ w: match whole words only      │
│ c/a: case-sensitive match      │
│ k/v: find in keys/values/both  │
│ /: exclude comments            │
│ r/.: regular expression        │
│ n/N: next/prev result          │
├────────────────────────────────┤
│ enter: select all              │
│ g: go to current find only     │
│ esc : cancel find              │
│ ? : toggle help text           │
╰────────────────────────────────╯
```

The top row has focus, so it's surrounded by a bold, colored rectangle. The chevrons (`>  <`) reiterate this fact (useful if color does not work).

In the top row, type the word you're searching for (for example "make").

Then you can either press the `ENTER` key to do the search, or press the `down arrow` to access the options (contorl/alt would have been nice but they do not work consistently in the terminal, hence this design choice).

If you press `ENTER`, all the places that match your search criteria will be selected as a multicursor.
Including, possibly, areas that are not visible because they are folded or offscreen.

If instead you press `down arrow` then you can access the options. They are:

- match whole words only
- case-sensitive search
- search only in keys, only in values, or both.
- regular expressions

You can use the `SPACE` key to toggle the option under the cursor and the `left` and `right` arrow keys to change which is focused.

You can also directly press their corresponding key (w, c, k, r).

The options should be fairly self-explanatory. You can press `?` (while on the second row) to toggle the on-screen descriptions.

You can press `ENTER` from there when you're happy with the choice, no need to return to the top row.

But you can also press `n` or `N` to navigate the finds (the cursor indicates the finds immediately as you type)

Finally, you can press `g` to keep only the current main selection (unselecting all the other matches), or `ESC` to cancel the find and return the cursor(s) to what they were before search.

## Sorting

In short, press "s" to sort and it should be fairly straight-forward. You want the cursor on the array or map
that contains the things you want to sort.

### A. Sorting lists of strings and numbers

Let's open the `sortme` test file (you can skip the build step if you already have the jar file).

```shell
mvn package -DskipTests
java -jar target/JavaJson-1.4-SNAPSHOT-jar-with-dependencies.jar testdata/sortme.json
```

Press `down arrow` once to move the cursor to the "numbers" section and press the `s` key.
This will open the sort dialog:

```
{                             ╭────────────[ SORT ]───────────────╮
>>"numbers": [ // 6 entries   ┏━━━┓────┬─────┬────────────────────┤     
    9                         > R < Aa │ num │                    │
    2                         ┗━━━┛────┴─────┴────────────────────┤
    5                         │ r : reverse order                 │                                     
    6                         │ a : separate upper/lowercase      │                                    
    91                        │ n : sort strings as numbers       │                                     
    0                         ├───────────────────────────────────┤                                      
  ]                           │ enter: sort                       │                                      
  "words": [ // 5 entries     │ esc : cancel                      │                                      
    "Mary"                    │ x: return to original order       │
    "had"                     │ ? : toggle help text              │
    "a"                       ╰───────────────────────────────────╯
```

When you press the `ENTER` key, it will sort the numbers by the cursor in ascending order.
If you pressed `r` first (or the `space bar`), then they are sorted in reverse order instead, from
large to small.

You can toggle the options by either pressing their key (`r`, `a`, `n`) or by using the `left` and `right` arrows
to select them and then `space bar` to toggle. I'll explain the "n" option shortly.

To close the dialog you either press `ENTER` to sort,
`ESC` to leave the list as it was when you opened the sort dialog,
or `x` to re-order the list to match the order of the input file.

You can also press `?` to show/hide the help text.

Now, the "n" option. It's useful when a string contains numbers. Consider this example:

```
>>"files on my hard disk": [ // 5 entries
    "homework"
    "homework 10"
    "homework 2"
    "homework 2b"
    "homework FINAL"
  ]
```

The result above is what happens when sorting lexicographically (the default option).
We can see that "10" comes before "2", as per the lexicographical rules. But of course
we usually prefer to see the files in numerical order, and that is what the "n" option
provides:

```
>>"files on my hard disk": [ // 5 entries
    "homework"
    "homework 2"
    "homework 2b"
    "homework 10"
    "homework FINAL"
  ]
```

Note that the "sort" dialog only work if at least one of the cursors is pointing
to a list. The non-list cursors will be ignored.

### B. Sorting lists of objects

It is also possible to sort lists that contain JSON objects. For example, in
the "sortme" file we opened in section A, if you go down to "records" and press `s`
you will notice the dialog is slightly different:

```
>>"records": [ // 4 entries
    {                                           ┏━━━━━━━━━━━━[ SORT ]━━━━━━━━━━━━━━━┓
      "name": "Bob"                             > difficulty                        <
      "score": 10                               ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
      "level": 5                                │ R │ Aa │ num │                    │
      "league": "small"                         ├───┴────┴─────┴────────────────────┤
      "streak": 5                               │ r : reverse order                 │
      "difficulty": "hard"                      │ a : separate upper/lowercase      │
      "ssn": "123-456-789"                      │ n : sort strings as numbers       │
    }                                           ├───────────────────────────────────┤
    {                                           │ enter: sort                       │
      "name": "Alice"                           │ esc : cancel                      │
      "score": 3                                │ x: return to original order       │
      "level": 3                                │ ? : toggle help text              │
      "league": "small"                         ╰───────────────────────────────────╯
```

The sort dialog now has two rows. A new row is present at the top, asking which
field you'd like to sort by. The options are what you can see in the records on
the screen: name, score, level, league, streak, difficulty, ssn. You can type those
in directly, or you can press the `TAB` key to be presented with a list of options.
Press `up/down` then `ENTER` to select an option, or `TAB` again to close the list.

### C. Sorting maps of strings/numbers

Sometimes, the things we want to sort are not in an array. Consider the example below:

```
>>"high scores": {                      ╭────────────[ SORT ]───────────────╮
    "Zebulon": 2                        │ (keys)                            │
    "Charlize": 10                      ┏━━━┓────┬─────┬────────────────────┤
    "Zanzibar": 20                      > R < Aa │ num │                    │
    "Aaron": 50                         ╰━━━┛────┴─────┴────────────────[?]─╯
  }
```

Here we have an associative map, where the key is the person's name, and the value is their score. Pressing `s`
there allows us to sort these too. The only difference is that instead of a field name, we get to choose between
"(keys)" to sort from Aaron to Zebulon, of "(values)" to sort by score, 2 to 50.

### D. Sorting maps of maps

Consider the example below: it's a key/value map, and it contains three named
maps: "thing 1", "thing 2", and "cat". Here the program gives us the choice
of sorting by keys (so "cat" would be first), or by the "name" field. If there
were other fields, they would be added to the list of course.

```
>>"named things": {
    "thing 1": {          ┏━━━━━━━━━━━━[ SORT ]━━━━━━━━━━━━━━━┓
      "name": "Bim"       ┃ (keys)                            ┃
    }                     ┗┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓┛
    "thing 2": {          │>(keys)                           <│
      "name": "Bem"       ╰┃name                             ┃╯
    }                      ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    "cat": {
      "name": "Cat In The Hat"
    }
  }
}
```

So in conclusion: roughly speaking, so long as you have something that looks like a sequence of things,
you should be able to sort them.

## Sibling selection

Select all the children of the current node by pressing `e` or `*`.
Press `ESC` to remove the secondary cursors.

While you have multiple cursors, all the operations you do (like fold, pin, sort, etc.) will apply
to all the cursors.

## Annotations

The viewer will in some cases add context to the existing document.
It is marked with a `//` symbol to signify that it is not part of the file on disk,
it's just here to aid readability.

Annotations include:

- Row count on lists
    
    ```
    [ // 1 entry
      {
        "vehicle": [ ... // 2 entries
    ```

- conversion from durations to natural units

    ```
      "recording": {
        "duration_sec": // 1.47 h
                        5300
    ```

- conversion from some date formats to local timezone

    ```
      "s_since_epoch": // 2021-11-20 05:36:58.000-0800
                       1637415418
      "created_at": // 2022-11-08 15:29:25.000-0800
                    "2022-11-08T23:29:25Z"
    ```

- color previews
    
    ```
      {
        "calendarId": "5a7e1"
        "color": "#f0f0f0" // ██
      }
    ```
  
## Unique keys

### Check keys are unique

Sometimes your JSON includes a list of repeated maps, all with the same structure. Something like this example:

```
[ // 1000 entries
  { "name": "Joe", "fave color": "blue" },
  { "name": "Alice", "fave color": "purple" }
  ...
```

Here I only show two, but there could be a lot of entries. How do you check that all the maps have "name"
and "fave color" spelled correctly? A typo could cause problems to whatever needs to process the JSON.

Move the cursor to the list (the `[` line), then
press the `a` key to open the "aggregates" menu.
Highlight "unique keys" (in this menu you use the up/down arrows to move the highlight)
and press `enter` (or press the shortcut key, `u`). This adds the "unique_keys()" annotation:

```
[ // 1000 entries
  // unique_keys() {
  //  =100% "name"
  //    99% "fave color"
  //     1% "fav color"
  // }
...
```

This tells you that most of the children have a "fave color" key, and one has "fav color" instead!
You found a typo! 

The count will say "=100%" when every single row has that value, to distinguish from a case with many
entries where almost all the rows have that value to the point that it rounds up to 100%.

You can navigate through this aggregate as normal, fold it, pin it, search it etc. 
You can press `shift-Z` to undo adding the aggregation (see also below).

The search dialog has an option called "exclude comments" you can use if you do not want the
results to include the aggregate information.

### Remove unique keys indication

You can remove the "unique_keys" indication by navigating to it (or the list that contains it), then
press `a` to open the aggregate menu and select "remove aggregate".

This will remove the whole "unique_keys" section. This will work even if you have done multiple
other operations since, whereas undo (`shift-Z`) will start by undoing the latest operation.

### Hierarchical unique keys

Consider the case where the things in your list have lists of their own.
Here is an example:

```
[ // 1000 entries
  {
    "name": "Zaphod",
    "friends": [
      {"name": "Arthur"},
    ]
  }
  ...
```

Can you check that all the friends have the "name" key? Yes!

If you enable "unique_keys()", you will see something like this:

```
[ // 1000 entries
  // unique_keys() {
  //   =100% "name"
  //     80% "friends": {
  //       =100% "name"
  //         30% "eye color"  
  // }
    
```

This means that all the entries have a "name" key, and 80% of them have "friends".
Considering only those that have a "friends" list, here all the friends have a "name"
key and 30% of them have an "eye color" field.

## Groupby

The "group by" feature allows you to organize a list of maps by grouping them based on
one of their keys. Here is an example, a list of cities and their corresponding country:

```
[ // 4 entries
  {
    "city": "Tokyo",
    "country": "Japan"
  },
  {
    "city": "Delhi",
    "country": "India"
  },
  {
    "city": "Mumbai",
    "country": "India"
  },
  {
    "city": "Metropolis"
  }
]
```

Maybe we're only interested in one country, or maybe we'd like to know how many
cities are in this list for each country. The `groupby` transformation can help.

In this case we'd like to group by country, so bring the cursor down to
one of the lines that says "country". You should see the `>>` symbol in the margin
on that line and the line should be highlighted.

Press `g` to call the `groupby` transformation.

The list of cities will now be replaced by a map with multiple lists: one per country.
It will look like this:

```
{ // grouped by country
  "Japan": [ // 1 entry
    {
      "city": "Tokyo"
>>    "country": "Japan"
    }
  ]
  "India": [ // 2 entries
    {
      "city": "Delhi"
      "country": "India"
    }
    {
      "city": "Mumbai"
      "country": "India"
    }
  ]
  "(null)": [ // 1 entry
    {
      "city": "Metropolis"
      "population": 11'000'000
    }
  ]
}
```

As you can see, there is a comment at the top indicating what we have done (group by
country), and then for each country there is a list of the cities from the original
list that belong to that country.

Everything that was in the original list will be kept, even if it doesn't have
a "country" key. Those will be shown in the `(null)` list, named this way
to indicate those don't have a value for the grouping key.

Grouping can be undone with the `shift-Z` key, like any other transformation.

If you have multiple cursors when you press `g`, then a groupby will be attempted
at each of the cursors.

## Parse JSON

When the cursor is on a line of text that happens to be JSON, you can press the
`ENTER` key to open the action menu; then select the "parse" option to
parse the line as JSON and make it part of the document.

Parsing can be undone with the `shift-Z` key.

# Related

Return to [README](../README.md).