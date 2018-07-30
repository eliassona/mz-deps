# mz_deps

A Clojure library designed to display dependencies.
(Internal project, not for public use)

## Usage

```clojure 
=> (load-file "src/mz_deps/totaldeps.clj")
=> (in-ns 'mz_deps.totaldeps)
=> (count jar-usage) ;this will take several minutes
725
```
Now jar-usage contains a list of all 3pps (725 of them in my case).
Look at them one at a time:
```clojure
=> (nth jar-usage 0) ;the first dependency
{:used-in-package #{"elasticsearch"}, :jar-file "/Users/anderse/src/mz-dev/mz-main/runtime/java/elasticsearch/lib/t-digest-3.0.jar", :name "t-digest", :version "3.0", :maven-id nil, :latest-version ("3.2_1" "3.2")}
=> (nth jar-usage 1) ;the second dependency
{:used-in-package #{"rtbs"}, :jar-file "/Users/anderse/src/mz-dev/mz-main/runtime/java/apache-axis-1.4/lib/commons-logging-1.0.4.jar", :name "commons-logging", :version "1.0.4", :maven-id nil, :latest-version ("1.1" "1.1" "0.0.15" "1.2.0.L0001" "1.2" "4.0.6" "5.5.23" "10.6.1" "1.1.0" "1.1.0" "1.0.4" "1.6.5" "1.0" "4.6.45" "4.6.45" "1.10.5" "2.0.3" "0.2.14" "1.0.4" "0.13.1" "10.6.1" "10.6.1" "4.6.45" "1.2.4" "1.2.4" "1.8.8" "1.0.140" "1.5.1" "0.2.14" "1.0.0.Final" "1.7.7" "1.0.3.Final" "1.4.61" "1.4.61" "1.4.61" "1.4.61" "1.1.1.v201101211721" "6.5.0.Final" "1.0.36" "1.1.1.v201101211721" "1.1.3" "9.0.0.v20130315" "9.0.0.v20130315" "1.0" "1.0.4-201003011305" "2.0.1" "1.0.0" "1.15" "1.3")}
```
Etc...

You can look at all dependencies at once by typing:
```clojure 
=> jar-usage
```
But that's probably overwelming!

You could dump the whole thing to an html table:
```clojure 
=> (spit "~/tmp/deps.html" (jar-usage->html))
nil
```


See how many jars are unused. Note! unused by the production code. They could still be used by some tool.
```clojure
=> (count (unused-jars))
2295
```

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
