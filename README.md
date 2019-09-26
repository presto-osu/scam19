This release includes the source code for the static analyses,
instrumentation and necessary scripts to reproduce the results
described in the SCAM'19 paper:

```
@inproceedings{zhang-scam19,
  title = " Introducing Privacy in Screen Event Frequency Analysis for {Android} Apps",
  author = " Hailong Zhang and Sufian Latif and Raef Bassily and Atanas Rountev",
  booktitle = "IEEE International Working Conference on Source Code Analysis and Manipulation",
  year = 2019,
} 
```

The directory structure is as follows:

- README.md: this file.

- INSTALL.md: [instructions](INSTALL) for reproduction of the results in the paper.

- LICENSE: license for the software components developed at OSU.

- apks: APKs of apps used in the SCAM'19 paper.

- db: recorded screen view events from Monkey runs as SQLite
  databases.

- code: source code for the static analysis and instrumentation, as
  well as scripts to run Monkey for simulation.

- simulation: scripts to read and randomize events in databases, and
  plot the results described in the paper.

- xml: results of the static analysis for the experimental subjects.

