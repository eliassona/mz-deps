(ns mz_deps.core
  (:use [clojure.set]
        [clojure.pprint])
  (:require [clojure.xml :refer [emit]]
            [clojure.spec :as s]
            [clojure.spec.test :as test]
            [clojure.string :as str]
            [clojure.set :refer :all])
  (:import [java.io File FileFilter]
           [java.util.zip ZipFile])
  (:gen-class))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-home "/Users/anderse/src/mz-dev/mz-main/mediationzone/core"
  #_(if-let [v (if-let [mz (java.lang.System/getenv "MZ_HOME")]
                mz
                (java.lang.System/getProperty "mz.home"))]
     v
     (throw (IllegalStateException. "mz-home is not set!")
     )))


(defn build-log-files 
  ([]
  (build-log-files (.getParentFile (File. mz-home))))
  ([f]
    (if (= (.getName f) "build")
      (let [bf (File. f "build.log")]
        (if (and (.exists bf) (.isFile bf))
          bf
          nil))
      (filter identity (flatten (map build-log-files (filter #(.isDirectory %) (.listFiles f))))))))

(defn parsed-content-of 
  "Parse a build.log file and return the parsed content as a map"
  [f]
  (reduce
    (fn [acc v] (-> acc 
                  (assoc :path (:package-path v))
                  (update :src #(conj % (format "src/%s/java" (:src-name v)) #_(format "src/%s/install" (:src-name v))))
                  (update :jars #(union % (into #{} (:libs v))))
                  ))
    {:src #{}, :jars #{}}
    (map 
      (comp
        #(update % :libs (fn [s] (vec (.split s ","))))
        (partial apply hash-map)
        (partial interleave [:package-path :package-name :src-name :java :jar :hyphen :todo :libs]) 
        (fn [s] (.split s ";"))) 
      (filter (fn [s] (not (.isEmpty s))) 
              (map #(.trim %) (.split (slurp f) "\n"))))))


(defn add-test-jars 
  "Add extra jars that are needed for testing etc."
  [jars]
  (conj jars "mediationzone/packages/ultra/build/jars/testsupport/mz-ULTRA-testsupport.jar"
             "mediationzone/core/lib/core_testsupport.jar" 
             "mediationzone/picostart/build/jars/testsupport/mz-PICOSTART-testsupport.jar"
;             "mediationzone/installation/build/jars/main/mz-INSTALLATION-main.jar"
             "runtime/java/testng/testng-6.8.jar" 
             "runtime/java/mockito-1.9.5/mockito-all-1.9.5.jar"
             "runtime/java/hamcrest/hamcrest-all-1.3.0RC2.jar"
             "runtime/tools/scalatest/2.2.6/scalatest_2.11-2.2.6.jar" 
             "runtime/java/apache-ant/lib/ant.jar"
             "runtime/java/easymock-3.0/easymock-3.0.jar"
             "runtime/java/clojure/clojure-1.6.0/clojure-1.6.0.jar"
             "runtime/java/junit4.3.1/junit-4.3.1.jar"))
(defn src-dir-exists? [package-dir]
  (fn [[kind path]]
    ;(dbg (format "kind %s, path %s" kind path))
    (if (= kind "src")
      (.exists (File. package-dir path))
      true)
    ))

(defn assoc-source-path 
  "assoc source path for all jars that have source"
  [m package-dir root-dir]
  (let [{:keys [kind path]} m
        assoc-fn 
        (fn [sp] 
          (if (.exists sp) 
            (assoc m :sourcepath (.getAbsolutePath sp))
            m))
        ]
    (condp = kind 
      "lib"
      (if (>= (.indexOf path "mediationzone") 0)
        (let [cp-dir (->> path File. .getParentFile)
              sp (File. (-> cp-dir .getParentFile .getParentFile .getParentFile) (->> cp-dir .getName (format "src/%s/java")))]
          (assoc-fn sp))
		    (let [sp (File. (format "%s-sources.jar" (.substring path 0 (- (count path) 4))))]
          (assoc-fn sp)))
      "con" m
      "src" m
      "output" m
    )))

(defn emit-classpath
  "Return the arg m in .classpath xml format
   
  "
  [m package-dir root-dir]
  (let [jars (add-test-jars (:jars m))]
    (emit {:tag :classpath 
           :content 
           (map (fn [[kind path]] {:tag :classpathentry :attrs (assoc-source-path {:kind kind, :path path} package-dir root-dir)}) 
                  (filter 
                    (src-dir-exists? package-dir) 
                    (partition 
                      2 
                      (concat
                        ["con" "org.eclipse.jdt.launching.JRE_CONTAINER" "con" "org.scala-ide.sdt.launching.SCALA_CONTAINER" "src" "test/src" "output" "idebuild"]
                        (interleave (take (count (:src m)) (repeat "src")) (:src m))
                        (interleave (take (count jars) (repeat "lib")) (map (partial format "%s/%s" root-dir) jars))))))})))


(defn emit-cp-for!
  "Emits the xml for eclipse .classpath.
   For example:
   cd to the component and the type
   (emit-cp-for) ;this will update 'package/couchbase/.classpath'
   For test-ng in eclipse: One solution to this problem (at least it worked for me) is to go to Window -> Preferences -> TestNG and uncheck Use project TestNG jar.
  "
  ([package-dir]
  (let [mz (.getParentFile (File. mz-home))
        root (.getParentFile mz)
        f (first (build-log-files package-dir))
        cp (File. package-dir ".classpath")]
    (println (format "Overwriting %s" (.getAbsolutePath cp)))
    (spit cp (with-out-str (emit-classpath (parsed-content-of f) package-dir root)))
    ))
  ([]
    (emit-cp-for! (File. "."))
    ))

(defn make-classpath-files 
  "under construction"
  []
  (map parsed-content-of (build-log-files)))


(defn -main [& args]
  (emit-cp-for!)
  )


(comment 
  (emit-cp-for! (File. (.getParentFile (File. mz-home)) "packages/couchbase"))
  )


(defn my-index-of [source search & opts]
  (apply str/index-of source search opts))
(s/fdef my-index-of
        :args (s/cat :source string? :search string?)
        :ref nat-int?
        :fn #(<= (:ret %) (-> % :args :source count)))

;;--------------------------------------------------------------------------------------------

(defn mzp-project? 
  ([dir build-file]
  (let [f (File. dir build-file)]
    (and (.isFile f) (.exists f))))
  ([dir] 
    (or (mzp-project? dir "build.xml")
        (mzp-project? dir "build.gradle"))))


(def no-recurs-set #{"lib" "build" "src" "test"})

(def recurse?
  (reify FileFilter 
    (accept [_ path-name]  
      (and (.isDirectory path-name) (not (contains? no-recurs-set (.getName path-name))))))) 

(declare mzps-of)

(defn sub-mzps-of [gradle? dir]
  (mapcat (partial mzps-of gradle?) (.listFiles dir recurse?))
  )

(defn mzps-of 
  ([gradle? dir]
    (concat
      (sub-mzps-of gradle? dir)
      (if (mzp-project? dir)
        (mzps-of (if gradle? (File. dir "build/libs") (File. dir "lib")))
        [])))
  ([dir]
    (.listFiles dir 
      (reify FileFilter 
        (accept [_ path-name] (and (.isFile path-name) (.endsWith (.getName path-name) ".mzp")))))))


(defn content-of [mzp]
  (into #{} (map #(.getName %) (enumeration-seq (.entries (ZipFile. mzp))))))

(defn mzp->content-map [gradle? dir]
  (into {} (map (fn [mzp] [(.getName mzp) (content-of mzp)]) (mzps-of gradle? dir))))


(defn diff-content [gradle ant]
  {:added (difference gradle ant)
   :missing (difference ant gradle)})

(defn key-set-of [v]
  (into #{} (keys v)))
                 

(defn diff-mzp [dir]
  (let [v1 (mzp->content-map true dir)
        v2 (mzp->content-map false dir)]
  (if (not= v1 v2)
    (let [ks1 (key-set-of v1) 
          ks2 (key-set-of v2)
          key-diff (diff-content ks1 ks2)
          dc {:content (into {} (map (fn [k] [k (diff-content (v1 k) (v2 k))]) (intersection ks1 ks2)))}]
      (if (empty? key-diff) dc (assoc dc :mzps key-diff)))
    "no difference")))




