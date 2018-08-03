(ns mz_deps.totaldeps
  (:require [clojure.set :refer [union]]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clojure.inspector :refer [inspect inspect-table ]]
            [hiccup.table :refer [to-table1d]]
            [hiccup.core :refer [html]])
  (:use [clojure.pprint])
  (:import [java.io File FileFilter]
           [java.util.jar JarFile]))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(defn maven-search "Search info about an artifact like for example 'jedis'" [name]
  ((-> (client/get (format "http://search.maven.org/solrsearch/select?q=%s&rows=10000&wt=json" name)) :body json/read-str) "response")) 

(defn latest-version-of 
  "Get the latest version of an artifact like for example 'jedis', the id is the group id and name concatenated with a colon, for example 'redis.clients:jedis'"
  [maven-id]
  (let [res ((maven-search maven-id) "docs")]
    (map #(% "latestVersion") res)))


(def mz-core (File. "/Users/anderse/src/mz-dev/mz-main/mediationzone/core"))

(def mz-home (-> mz-core .getParentFile))

(def installed-home "/Users/anderse/src/mz-dev/mz-main/mediationzone/mzhomes/anders")

#_(def mz-home 
   (if-let [mz (get (System/getenv) "PROJECT_HOME")]
     (File. mz)
     (throw (IllegalStateException. "PROJECT_HOME is not set"))))

#_(def installed-home 
   (if-let [mz (get (System/getenv) "MZ_HOME")]
     (File. mz)
     (throw (IllegalStateException. "MZ_HOME is not set"))))

(def common-lib (File. installed-home "common/lib"))

(defn ends-with-filter [suffix] 
  (reify FileFilter (accept [_ f] (and (.isFile f) (.endsWith (.getAbsolutePath f) suffix)))))

(def jar-filter (ends-with-filter ".jar"))

(def mzp-filter (ends-with-filter ".mzp"))


(def common-lib-set (set (.listFiles common-lib jar-filter)))

(def common-lib-set-names (set (map #(.getName %) common-lib-set)))
(def mz-root (-> mz-home .getParentFile))

(def mz-runtime (File. mz-root "runtime"))

(def mzp-home (File. installed-home "codeserver/packages/active"))

(def mzp-set (set  (.listFiles mzp-home mzp-filter)))

(def dir-filter (reify FileFilter (accept [_ f] (.isDirectory f))))

(defn all-dirs-of [dir]
  (conj 
    (mapcat 
      all-dirs-of 
      (.listFiles dir 
        dir-filter)) dir))

(defn dep-file-of [dir] (File. dir "deps.clj"))

(defn dep-files-of [dirs] (filter #(.exists %) (map dep-file-of dirs)))

(defn eval-deps [f] (-> f slurp read-string eval))

(def dep-map 
  "A map where the keys are the component names and the values are the corresponding dependencies"
  (reduce (fn [acc v] (merge acc v)) (map eval-deps (dep-files-of (all-dirs-of mz-home)))))


(def jar-set "MZ all jars dependencies" (into #{} (filter #(.endsWith % ".jar") (apply union (-> dep-map vals)))))

(def runtime-path (.getAbsolutePath mz-runtime))

(defn is-3pp? [p] (not (.startsWith p (.getAbsolutePath mz-home))))

(def jar-set-3pp "A set of all 3pp jars" (set (filter is-3pp? jar-set)))
(def jar-set-3pp-names "A set of all 3pp jar names" (set (map #(.getName (File. %)) jar-set-3pp)))


(defmacro try-wrapper [code]
  `(try 
     ~code
     (catch Exception e#
       "Couldn't figure out this info")))

(defn jar->mf [path]
  (-> path JarFile. .getManifest)
  )

(defn mf-attrs-of [path]
  (if-let [mf (jar->mf path)]
    (-> mf .getMainAttributes)))

(defn maven-id-of [path]
  (if-let [attrs (mf-attrs-of path)]
    (-> attrs (.getValue "Bundle-SymbolicName"))))

(defn strip-path [p]
  p
  #_(let [l (inc (count runtime-path))]
     (.substring p l)))
(defn only-3pp? [n]
  (contains? jar-set-3pp-names (.getName (File. n)))) 

(defn mzp->packaged-3pp-jars 
  "List all 3pp jars packaged in this mzp"
  [mzp]
  (set 
    (map #(.getName (File. %)) (filter #(and (.endsWith % "jar") (only-3pp? %)) (keys (.getEntries (jar->mf mzp)))))))

(def mzp->packed-3pp-jar-map "A map of mzps and their packaged 3pp jar"
  (into {} (map (fn [x] [x (mzp->packaged-3pp-jars x)]) mzp-set)))

(defn jar->mzp "Which mzp(s) is this jar contained in" [jar]
  (set (map first (filter (fn [e] (contains? (val e) jar)) mzp->packed-3pp-jar-map))))

(defn kind-of [jar]
  (let [common (contains? common-lib-set-names jar)
        mzp (jar->mzp jar)]
    (cond
      (and common (not (empty? mzp)))
      [:common mzp]
      common
      :common
      (not (empty? mzp))
      mzp
      :else
      :none)))

(defn detail-info-of [jar pks]
  (let [name-version-ix (.lastIndexOf jar "-")
        name-start (inc (.lastIndexOf jar "/"))
        name (try-wrapper (.substring jar name-start name-version-ix))
        maven-id (maven-id-of jar)]
    {:used-in-package (set pks), 
     :jar-file (strip-path (dbg jar)), 
     :name name, 
     :version (try-wrapper (.substring jar (inc name-version-ix) (.lastIndexOf jar ".")))
     :maven-id maven-id
     :kind (kind-of (.getName (File. jar)))
     :latest-version (try-wrapper (latest-version-of (if maven-id maven-id name)))})) 

(defn jar-usage-of [jar]
  (detail-info-of jar (filter #(contains? (dep-map %) jar) (keys dep-map))))

(def jar-usage 
  "A list of maps containing info about each 3pp jar" 
  (map jar-usage-of jar-set-3pp))



(defn regex-search [p] 
  (let [p (format "(?i).*%s.*" p)]
    (fn [s] (.matches s p))))
      

(defn search-for 
  "Search for a string contained in the jar filename"
  [s]
  (let [rs (regex-search s)]
    (filter #(rs (:jar-file %)) jar-usage)))



(defn unused-jars []
  (let [rt-jars (map #(.getAbsolutePath %) (mapcat #(.listFiles % jar-filter) (all-dirs-of mz-runtime)))]
    (dbg (count rt-jars))
    (filter (complement (partial contains? jar-set-3pp)) rt-jars)))


(defn jar-usage->html []
  (let [attr-fns {:table-layout "auto" }]  
    (html 
      (to-table1d jar-usage 
      [:jar-file "Jar", 
       :name "Name", 
       :version "Version" 
       :maven-id "Maven Id", 
       :latest-version "Latest Version"
       :used-in-package "Used in Package", 
       ]
      attr-fns))))
