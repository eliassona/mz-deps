(ns mz_deps.gradle
  (:import [java.io File])
  (:require [instaparse.core :as insta]
          [commentclean.core :as comment])

  )


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-home "/Users/anderse/src/mz-dev/mz-main/mediationzone")

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

(defn ast->clj-map [state]
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


(defn ast->clj [ast]
  (let [state (atom {})]
    (insta/transform (ast->clj-map state) ast)
    @state))

(defn dep-of [f]
  (print (.getName f))
  (let [ast (-> f slurp comment/clean (parser :start :context))]
    (if (insta/failure? ast)
      (throw (IllegalStateException. (.getName f)))
      (ast->clj ast))))

(defonce dep-list (map dep-of (get-extdep-metadata)))

(def dep-as-map (reduce (fn [acc v] (assoc acc (v :symbol) (v :jars))) {} dep-list))



