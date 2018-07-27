(ns mz_deps.totaldeps
  (:require [clojure.set :refer [union]])
  (:use [clojure.pprint])
  (:import [java.io File FileFilter]))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-core (File. "/Users/anderse/src/mz-dev/mz-main/mediationzone/core"))

(def mz-home (-> mz-core .getParentFile))

(def mz-root (-> mz-home .getParentFile))

(def mz-runtime (File. mz-root "runtime"))

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

(defn is-3pp? [p] (.startsWith p runtime-path))

(def jar-set-3pp "A set of all 3pp jars" (filter is-3pp? jar-set))

(defmacro try-wrapper [code]
  `(try 
     ~code
     (catch Exception e#
       "Couldn't figure out this info")))

(defn detail-info-of [jar pks]
  (let [name-version-ix (.lastIndexOf jar "-")
        name-start (inc (.lastIndexOf jar "/"))]
    {:used-in-package (set pks), 
     :jar-file jar, 
     :name (try-wrapper (.substring jar name-start name-version-ix)), 
     :version (try-wrapper (.substring jar (inc name-version-ix) (.lastIndexOf jar ".")))})) 

(defn jar-usage-of [jar]
  (detail-info-of jar (filter #(contains? (dep-map %) jar) (keys dep-map))))

(def jar-usage (map jar-usage-of jar-set-3pp))

(defn regex-search [p] 
  (let [p (format "(?i).*%s.*" p)]
    (fn [s] (.matches s p))))
      

(defn search-for [s]
  (let [rs (regex-search s)]
    (filter #(rs (:jar-file %)) jar-usage)))

