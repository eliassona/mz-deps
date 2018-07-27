(defproject mz_deps "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha9"]
                 [org.clojure/test.check "0.9.0"]
                 [instaparse "1.4.7"]
                 [clj-http "3.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 [hiccup-table "0.2.0"]
                 [hiccup "1.0.5"]
                 ]
  :java-source-paths ["java/src"]

  :main mz_deps.core)
