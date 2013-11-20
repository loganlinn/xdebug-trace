(ns xdebug-trace.line
  (:refer-clojure :exclude [time]))

(defn level [line] (nth line 0))
(defn fn-num [line] (read-string (nth line 1)))
(defn entry-line? [line] (= (nth line 2) "0"))
(defn time [line] (read-string (nth line 3)))
(defn memory [line] (read-string (nth line 4)))

;; Entry line fields
(defn fn-name [line] (nth line 5))
(defn user-defined? [line] (= (nth line 6) "1"))
(defn file [line] (nth line 8))
(defn line-num [line] (read-string (nth line 9)))

(defn line? [line] (and (seq line) (#{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9} (ffirst line))))
