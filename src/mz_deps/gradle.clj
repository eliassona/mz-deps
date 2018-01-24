(ns mz_deps.gradle
  (:use [clojure.pprint])
  (:import [java.io File])
  (:require [instaparse.core :as insta]
          [commentclean.core :as comment])

  )


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-home "/Users/anderse/src/mz-dev/mz-main/mediationzone")

(def mz-main (-> mz-home File. .getParentFile .getAbsolutePath))

(defn get-extdep-metadata	[]
  (filter #(.endsWith (.getName %) ".conf") (.listFiles (File. mz-home "extdep-metadata"))))


(def parser (insta/parser (clojure.java.io/resource "gradledsl.bnf")))


(defn closure-fn [state]
  (fn [& args]
    (condp = (first args)
      "id"
      (swap! state assoc :symbol (-> args rest second first second))
      "jars"
      (swap! state assoc :jars (map #(if (instance? java.util.Map %) (read-string (% "path")) "") (-> args rest first first second)))
      :else
    )))


(defn partial-apply [& x]
  (apply str x))

(defn ast-dep->clj-map [state]
  {:charValue identity
   :symbol partial-apply
   :strLit (fn [& args] args);(partial apply str)
   :ident partial-apply
   :value identity
   :array (fn [& args] args)
   :property (fn [k v] [k v])
   :closure (closure-fn state)
   :map (fn [& args] (into {} args))
   :context (fn [& args] args)
   })


(defn ast-dep->clj [ast]
  (let [state (atom {})]
    (insta/transform (ast-dep->clj-map state) ast)
    @state))

(defn dep-of [f]
  (print (.getName f))
  (let [ast (-> f slurp comment/clean (parser :start :context))]
    (if (insta/failure? ast)
      (throw (IllegalStateException. (.getName f)))
      (ast-dep->clj ast))))

(defonce dep-list (map dep-of (get-extdep-metadata)))

(def dep-as-map (reduce (fn [acc v] (assoc acc (v :symbol) (v :jars))) {} dep-list))


(defn add-mz-dependency [state]
  (fn [& args]
      (swap! state update :mz-dep #(if % (conj % (first args)) [(first args)]))))

(defn add-unit-dependency [state]
  (fn [& args]
    (let [m (into {} args)]  
      (swap! state update :unit-dep #(if % (conj % m) [m])))))

(defn ast-build->clj-map [state]
  {
   :charValue identity
   :symbol partial-apply
   :strLit partial-apply;(fn [& args] args);(partial apply str)
   :ident partial-apply
   :value identity
;   :array (fn [& args] args)
;   :property (fn [k v] [k v])
;   :closure (closure-fn state)
;   :map (fn [& args] (into {} args))
;   :context (fn [& args] args)
    :mz-dep (add-mz-dependency state)
    :unit-dep (add-unit-dependency state)
    :map-item (fn [k v] [(keyword k) v])
   })

(defn ast-build->clj [ast]
  (let [state (atom {})]
    (insta/transform (ast-build->clj-map state) ast)
    @state))


(defn unit-path-of [m]
  (let [name (:component m)
        unit (:unit m)
        path-fn #(format "%s/%s/build/jars/%s/mz-%s-%s.jar" % name unit (.toUpperCase name) unit)
        exist-fn #(let [p (path-fn %)] (when (.exists (File. (path-fn %))) p))]
    (if-let [p (exist-fn mz-home)]
      p
      (exist-fn (format "%s/packages" mz-home)))))

(defn abs-deps-of [m]
  (let [abs-path (partial format "%s/runtime/java/%s" mz-main)]
    (concat 
      (map unit-path-of (:unit-dep m))
      (map abs-path (mapcat dep-as-map (:mz-dep m))))))

(defn src-path-of [dir]
  (map (fn [d] (.getAbsolutePath d)) (filter #(.isDirectory %) (.listFiles (File. (format "%s/%s/src" mz-home dir))))))

(defn all-paths [dir]
  {:src (src-path-of dir)
   :deps (abs-deps-of (ast-build->clj (parser (comment/clean (slurp (format "%s/%s/build.gradle" mz-home dir))) :start :context)))})



