(ns xdebug-trace.core
  (:require [xdebug-trace.reader :as reader]
            [xdebug-trace.view.trace :as view.trace]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn -main [& [path limit offset]]
  (let [limit (if limit (Long/parseLong limit) 1000)
        offset (if offset (Long/parseLong offset) 0)]
   (with-open [rdr (io/reader path)]
    (->> (reader/trace-line-seq rdr)
         (drop offset)
         (take limit)
         (reader/read-trace)
         (println)))))
