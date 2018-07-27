(ns mz_deps.diff
  (:require [clojure.set :refer :all])
  (:import [java.io File FileFilter]
           [java.util.zip ZipFile]))

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
