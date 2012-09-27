GoodOD Ontology Evaluator
=========================
Copyright (C) 2011--2012 The University of Rostock.

Installation and Usage Instructions
-----------------------------------
* Get a working maven installation to build the evaluator.
* Install the HermiT reasoner into your maven repository using
    `mvn install:install-file -DgroupId=com.hermit-reasoner -DartifactId=HermiT
     -Dversion=1.3.5 -Dpackaging=jar -Dfile=HermiT.jar`
* Install the [OntoSim package](https://gforge.inria.fr/projects/ontosim/) 
  into your maven repository using
    `mvn install:install-file -DgroupId=org.inrialpes.exmo -DartifactId=OntoSim
     -Dversion=2.2 -Dpackaging=jar -Dfile=ontosim.jar`
* Install the OntoWrap package from the [AlignAPI](http://alignapi.gforge.inria.fr)
  into your maven repository using
    `mvn install:install-file -DgorupId=org.iniralpes.exmo -DartifactId=OntoWrap
     -Dversion=4.3 -Dpackaging=jar -Dfile=ontowrap.jar`
* Install the [secondstring package](http://secondstring.sourceforge.net) into
  your maven repositry using
    `mvn install:install-file -DgroupId=com.wcohen -DartifactId=secondstring
     -Dversion=SNAPSHOT -Dpackaging=jar -DFile=secondstring.jar`
* Run `mvn package` to build and package the evaluator. The corresponding .jar
  files are placed in `./target/`. The one with the `-eval-assembly` suffix also
  contains all dependencies.
* [Only for GoodOD internal use:] Set the `GOODOD_REPO_ROOT` environment variable
  (with `setenv`/`export` on *nix or `set` on Win32, for permanently setting it
  on Windows, please refer to to
  [this document](http://support.microsoft.com/kb/310519)) to the directory
  containing the GoodOD svn repository.
* Run the jar file with `java -jar` and the test description plist or two owl
  files as arguments. The `-h` switch will display further usage instructions.

Scope of Classes from the GoodOD Ontology Evaluator
---------------------------------------------------

Some of the classes from the packages `de.uni_rostock.goodod.checker` and
`de.uni_rostock.goodod.evaluator` are specific to evaluation tasks in the GoodOD
project, but can be used for generic uses. The classes from
`de.uni_rostock.goodod.owl` are, however, completely  independent of GoodOD
specifics and can be reused for other similarity measurement tasks.

Configuration
--------------

The file `src/main/resources/config.plist` contains plain text configuration for
the package. Configuration files can also be supplies using the `-c` command
line switch. The relevant configuration values are the following:

* threadCount:	specifies the number of threads to use for normalization and
               	measurement.
* similarity:	The (local) similarity measurement algorithm to use. Available
             	values are:
 - SC: Semantic Cotopy (structural, Dellschaft/Staab 2006)
 - CSC: Common Semantic Cotopy (structural, Dellschaft/Staab 2006)
 - CosineVM: Cosine Vector Measure (lexical, Euzenat et al. 2009)
 - TripleBasedEntitySim: Triple Based Entity similarity (structural, Euzenat et al. 2009)
 - The qualified name of any class implementing the
 `de.uni_rostock.goodod.owl.comparison.Comparator` interface.
* ignoredImports: The import IRIs to ignore for ontologies being compared.
* normalizers: 	This subtree contains the configurations for the normalizers. In
		the 'Basic' normalizer, you can reroute imports of ontologies by
		specifying the key value pairs. The 'chain' key for the
		NormalizerChain specifies the names of normalizers to run in turn.
* measures:	Configuration for similarity measurement classes. The only available
		option here is for triple based entity similarity, where you can
		choose between three aggregation schemes (MaxCoupling, AverageLinkage,
		Hausdorff).

License
-------

The software is made available under the GNU General Public License (GPL),
version 3  or later (see LICENSE).
