(ns mz_deps.gradle
  (:use [clojure.pprint])
  (:import [java.io File])
  (:require [instaparse.core :as insta]
            [clojure.xml :refer [emit]]
          #_[commentclean.core :as comment])

  )


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def mz-home "/Users/anderse/src/mz80-dev/mz-main/mediationzone")

(def mz-main (-> mz-home File. .getParentFile .getAbsolutePath))


(defn src-path-of [dir]
   (map (fn [d] (format "src/%s/java" (.getName (.getParentFile d)))) (filter #(and (.exists %) (.isDirectory %)) (map #(File. % "java") (.listFiles (File. dir "src"))))))

(defn path-entry-of [kind e]
  {:tag :classpathentry :attrs {:kind kind, :path e}})

(defn src-entry-of [e] (path-entry-of "src" e))

(defn file-exists? [fn] (.exists (File. fn)))


(defn src-dirs-from-component [src-dir]
  (if (= (.getName src-dir) "bin")
    []
    (map #(.getAbsolutePath %) (filter #(.exists %) (map (fn [p] (File. p "java")) (.listFiles src-dir))))))

(defn src-dirs-of [dir]
  (let [src-dir (File. dir "src")
        test-dir (File. dir "test/src")]
    (concat (src-dirs-from-component src-dir)
            (flatten (map src-dirs-of (filter #(.isDirectory %)  (.listFiles dir))))
            (if (.exists test-dir) [(.getAbsolutePath test-dir)] []))))


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

(defn emit-classpath-mz [dir]
  (let [ix (inc (count (.getAbsolutePath dir)))]
    (emit {:tag :classpath 
         :content
           (conj (map (comp src-entry-of #(.substring % ix)) (src-dirs-of dir)) 
                 (path-entry-of "output" "idebuild/classes")
                 (path-entry-of "con" "org.eclipse.jdt.launching.JRE_CONTAINER")
                 (path-entry-of "con" "org.scala-ide.sdt.launching.SCALA_CONTAINER"))})))

(defn emit-cp-for! [dir]
  (let [d (File. (format "%s/%s" mz-home dir))
        _ (assert (.exists d))
        eclipse-cp-file (File. d ".classpath")]
    (spit eclipse-cp-file (with-out-str (emit-classpath d)))
    (println (format "New classpath written to %s" eclipse-cp-file))))

(defn emit-cp-mz-for! [dir]
  (let [d (File. dir)
        _ (assert (.exists d))
        eclipse-cp-file (File. d ".classpath")]
    (spit eclipse-cp-file (with-out-str (emit-classpath-mz d)))
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

(defn facit-files-of ([dir]
  (filter #(>= (.indexOf (.getAbsolutePath %) "facit") 0) (.listFiles dir))
  )
  ([]
    (flatten (map facit-files-of (filter #(.isDirectory %) (.listFiles (File. mz-home "testcase/test/src/com/digitalroute/transport")))))))


(def id-map 
  {"017BB0B8FB061C1876D43DD75468AA40" "9BA0CE9FBC115A6921E435A58DB6CA2D"
   "5812CC9055F6C4CF78A1A615D741F00E" "54BF8B40290C8B45515AF1137B0E313E"
   "442EAA11FC0E7F65DD9E7A0E6B4E5160" "7E8F2BC1AFF99E54553AB11F73AA9D21"
   "F649149AA54736C5D7DE8C93C703FF04" "5CDB297B37F4C76FE9A58EF7FD477082"
   "9E5885A757778BFB153C6C877A7D9A86" "621E5D083FAEFAA51D00F0E8D03DA387"
   "803E34B530D6F396D427B3825869DDAF" "20195CBF6CB61FB3E4FEF7F19FAC571D"
   "4E79163B01BCB86CD9C3619B7F17C640" "3E459C4CBCD24E70E2C8562D8A98717D"
   "1D70FC3C24FFD64DD537656316C71E91" "09D29C46FE9EAC6EC70FC997F48D682D"
   "48851644227183C2041D838568E117EC" "901DFCB735BF00339861418BC05760FA"
   "EDFC3238C9DBC6155AE798C265915B5E" "AD5DD738631C6762872D999C672738E2"
   "CF9496C386D484295DBA3F7AAF872FE6" "6B2E5EA49F6AEE794B8D4A1F8D296AC4"
   "ABE0496FDA3C6D5719F929A18F81632B" "78E1382C533B11C2C4BCDF1DA3565B31"
   "183A23CCAC291CEED762DE466D1E4E39" "D4C7021975B1B4FFB724D2AADCAAC42F"
   "CD7D297DCD02EE31796532DDEE70A147" "29CF3981957BA27995539660F67C2DBF"
   "1D2E6A059AF8120841E62C87CFDB3FF4" "4584186C66322468B520177D1BF42398"
   "1E0347C5B625D03EEE35B0583C466E00" "CB74689D3174AD2257DE2FFA5DC8CEE6"
   "759904B4775B729E7A5AF00D93C97817" "45EA0AB6AAD20D054EDBB1434D91A84E"
   "150550C60F29F472A71CD162CF0D76A2" "9FF0488C382586D2461D1A4E223D6A64"
   "BEAFC6A0E9DACA43550BA9037DDED443" "DF520EE55DE3ACC7BBF6623294BE92EF"
   "E9BA380C43A51B918DD225BBB3BC5B67" "DFEDA3A473D13180F7E64AE77634F927"
   "EC45CEBD6C22E056D69F92D264D355B3" "81EC7D167B97624DAC718EC317833EBC"
   "45956A0C71DAF51A658156F9D215746B" "42757F8480B642370AF64BD6AD6D7EE3"
   "D176D4B5B6ACFBCBD9FCA5C8B9E17455" "6D3D69B87AD2ED4B2F37B48649EFDE6F"
   "A000A150CA8647B8A926001359F27B0E" "5261EF186046CBA633F24AFB25F67188"
   "63A237D2247505BE378B8E0D6BEA174F" "56BD013179F703614E833967E142DBA7"
   "BF1AEC7D2ED2DEF13D3845BB25936C89" "FF897D6B574248172F3D5E372DC10CB7"
   "EB8652FA3B329B87F59080BC44047B0C" "2EF4D9BC167C1EB4B10F41AAE574476F"
   "DACAF5E392725E413EBADC2C15994BA4" "8EC3819D45CA00A7A6029E971FF3A8CA"
   "D98C485E37D751C3319814B675249B52" "4558B01718953D31D708BBACCED57B91"
   "7D152A5D30F4B8F5F7F62772C8862DFA" "2BCE80FE227A94A58A26D51C811917CF"
   "7F5E3F81E1CD2B67B61AC9F9E578E8FD" "344B1E60CA6DD08E270527E99183332E"})



(defn id-str-of [id] 
  (format "DR_DEFAULT_KEY-%s" id))

(defn key-id->val-id [id]
  (if-let [new-id (id-map id)]
    new-id
    (throw (IllegalStateException. id))))

(defn replace-id [acc k] 
  (.replace acc (id-str-of k) (id-str-of (key-id->val-id k))))

(defn new-ids-of [text]
  (reduce replace-id text (keys id-map)))

(defn replace-all-ids! []
  (doseq [f (facit-files-of)]
    (let [text (slurp f)
          new-text (new-ids-of text)]
      (println (= new-text text))
      (spit f new-text))
    )
  )

(comment 
  example
  (emit-cp-for! "packages/ultra_gpb")
  )
