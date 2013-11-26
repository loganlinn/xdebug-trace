(ns xdebug-trace.line
  (:refer-clojure :exclude [time]))

(defn level [line] (nth line 0))
(defn fn-num [line] (some-> (nth line 1) read-string))
(defn entry-line? [line] (= (nth line 2) "0"))
(defn time [line] (some-> (nth line 3) read-string))
(defn memory [line] (some-> (nth line 4) read-string))

;; Entry line fields
(defn fn-name [line] (nth line 5))
(defn user-defined? [line] (= (nth line 6) "1"))
(defn file [line] (nth line 8))
(defn line-num [line] (some-> (nth line 9) read-string))
(defn arguments [line] (drop 11 line))

(def ^:private digits #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9})

(defn line? [line]
  (and (seq line) (digits (nth line 0))))

(defn trace-start? [line]
  (.startsWith ^String (nth line 0) "TRACE START"))

(defn trace-end? [line]
  (.startsWith ^String (nth line 0) "TRACE END"))

(defn date [line]
  (if-let [date-str (re-find #"(?<=\[)\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?=\])" (nth line 0))]
    date-str))
