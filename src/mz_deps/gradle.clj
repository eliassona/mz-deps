(ns mz_deps.gradle
  (:use [clojure.pprint])
  (:import [java.io File])
  (:require [instaparse.core :as insta]
            [clojure.xml :refer [emit]]
          [commentclean.core :as comment])

  )


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-home "/Users/anderse/src/mz-dev/mz-main/mediationzone")

(def mz-main (-> mz-home File. .getParentFile .getAbsolutePath))


(defn src-path-of [dir]
   (map (fn [d] (format "src/%s/java" (.getName (.getParentFile d)))) (filter #(and (.exists %) (.isDirectory %)) (map #(File. % "java") (.listFiles (File. dir "src"))))))

(defn path-entry-of [kind e]
  {:tag :classpathentry :attrs {:kind kind, :path e}})

(defn src-entry-of [e] (path-entry-of "src" e))

(defn file-exists? [fn] (.exists (File. fn)))


(defn emit-classpath [dir]
  (let [deps (File. dir "dependencies.edn")
        _ (assert (.exists deps))
        deps (filter file-exists? (-> deps slurp read-string))]
    (emit {:tag :classpath 
         :content
         (concat
           (conj (map src-entry-of  (src-path-of dir)) 
                 (src-entry-of "test/src")
                 (path-entry-of "output" "idebuild/classes")
                 (path-entry-of "con" "org.eclipse.jdt.launching.JRE_CONTAINER")
                 (path-entry-of "con" "org.scala-ide.sdt.launching.SCALA_CONTAINER"))
           (map (partial path-entry-of "lib") deps)
           )})))

(defn emit-cp-for! [dir]
  (let [d (File. (format "%s/%s" mz-home dir))
        _ (assert (.exists d))
        eclipse-cp-file (File. d ".classpath")]
    (spit eclipse-cp-file (with-out-str (emit-classpath d)))
    (println (format "New classpath written to %s" eclipse-cp-file))))    
    
;the following gradle code is needed in the build.gradle

;task printDeps {
;   doLast {
;   		writeDeps()
;   }
;}

;def writeDeps() {;
;	file('./dependencies.edn').withWriter('UTF-8') { writer ->
;	   writer.write '['
;	   project.configurations.testRuntimeClasspath.each { 
;			writer.write '"'
;			writer.write it.getAbsolutePath()
;			writer.write '"'
;			writer.write '\n' 
;		}
;	   writer.write ']'
;	}
;}


(comment 
  example
  (emit-cp-for! "packages/ultra_gpb")
  )
