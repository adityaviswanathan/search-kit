## Prerequisites
- Python 2.7 (comes bundled with Mac OS or install with ```brew install python```)
- Java 1.8
- Maven 3.5 (to build the project)

## Build search index
Manage which data you will build a search index over by setting params in `..lucene/file/LuceneWriteIndexFromFile.java` 

To build an index over Wikipedia data, set these params:

```
boolean WIKIFLAG = true;
String wikiData = "<PATH/TO/.BZ2/WIKI/DUMP>"
```

To build an index over an arbitrary set of files, set these params:

```
boolean PATHFLAG = true;
String docsPath = "<PATH/TO/FILES>"; 
```

Then, build and run the whole project by setting the correct `main` entrypoint in the Maven `pom.xml`. In particular, make these changes:

```
.
.
.
<configuration>
  <mainClass>com.thoughtspot.lucene.file.LuceneWriteIndexFromFile</mainClass>
</configuration>
.
.
.
```
Assuming you have the prerequisites installed, run the project using `mvn clean dependency:copy-dependencies package && mvn -X -e exec:java`. If the run was successful, the index will be written to the path specified in the params in `..lucene/file/LuceneWriteIndexFromFile.java`. 

## Word window
Get the word window and corresponding word frequencies using `..lucene/highlight/LuceneSearchHighlighter.java`

To search for a term, set this param:

```
.
.
.
//Query parser to be used for creating TermQuery
QueryParser qp = new QueryParser("contents", analyzer);

//Create the query
Query query = qp.parse("<SEARCHTERM>");

//Search the lucene documents
TopDocs hits = searcher.search(query, 10);
.
.
.
```
Then, build and run the whole project by setting the correct `main` entrypoint in the Maven `pom.xml`. In particular, make these changes:

```
.
.
.
<configuration>
  <mainClass>com.thoughtspot.lucene.highlight.LuceneSearchHighlighter</mainClass>
</configuration>
.
.
.
```
Then run the project once aagain with the `main` pointed at the `Highlighter` module using `mvn clean dependency:copy-dependencies package && mvn -X -e exec:java`. The results will be printed to stdout. For example, when an index is built on the Wikipedia article for 'anarchism', the following results are obtained:

```
====================
Search hit fragments
====================

 from the word "<B>anarchy</B>" and the suffix "-ism", themselves derived respectively from the Greek , i.e
. "<B>anarchy</B>" (from , "anarchos", meaning "one without rulers"; from the privative prefix ἀν
 tradition. Georges Lechartier wrote that "The true founder of <B>anarchy</B> was Jesus Christ and ... the first anarchist
 <B>anarchy</B> as their end and consequently refrain from committing to any particular method of achieving it. The Spanish
 <B>anarchy</B>" where order arises when everybody does "what he wishes and only
 <doc id="1" url="?curid=1" title="Redirect2AnarchistAnarchiststhe"> Redirect2AnarchistAnarchiststhe  "  Anarchism is a political
 philosophy that advocates self-governed societies based on voluntary institutions. These are often described as stateless
 societies, although several authors have defined them more specifically as institutions based
 on non-hierarchical free associations. Anarchism holds the state to be undesirable, unnecessary, and harmful. While
 anti-statism is central, anarchism generally entails opposing authority or hierarchical organisation in the conduct
 of all human relations, including, but not limited to, the state system. Other forms of authority it opposes
 include patriarchal authority, economic domination through private property, and racist supremacy. Anarchism
 is usually considered a radical left-wing ideology, and much of anarchist economics and anarchist legal
 philosophy reflects anti-authoritarian interpretations of communism, collectivism, syndicalism, mutualism, or participatory economics
. Anarchism does not offer a fixed body of doctrine from a single particular world view
, instead fluxing and flowing as a philosophy. Many types and traditions of anarchism exist, not all of which
 are mutually exclusive. Anarchist schools of thought can differ fundamentally, supporting anything from
 extreme individualism to complete collectivism. Strains of anarchism have often been divided into the categories
 of social and individualist anarchism or similar dual classifications. The term "anarchism" is a compound word composed
- ("an-", i.e. "without") and , "archos", i.e. "leader", "ruler"; (cf. "archon" or , "arkhē", i.e. "authority
", "sovereignty", "realm", "magistracy")) and the suffix or ("-ismos", "-isma", from the verbal infinitive suffix -ίζειν
, "-izein"). The first known use of this word was in 1539. Various factions within the French Revolution
 labelled opponents as anarchists (as Robespierre did the Hébertists) although few shared many views
 of later anarchists. There would be many revolutionaries of the early nineteenth century who contributed to the anarchist
 doctrines of the next generation, such as William Godwin and Wilhelm Weitling, but they did not use the word "anarchist

================================================
Aggregate word frequencies for window of size 10
================================================

{=36, entails=1, been=1, thought=1, patriarchal=1, mutually=1, nonhierarchical=1, without=2, isma=1, these=1, offer=1, would=1, ...
``` 


