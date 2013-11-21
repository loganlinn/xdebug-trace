(ns xdebug-trace.core
  (:require [xdebug-trace.line :as l]
            [clojure.pprint :refer [pprint]]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.io.Reader))

(def test-data '(["8" "436247" "0" "5.331024" "40495216" "HuddlerObject->__get" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/Modules.php" "97" "1" "string(10)"] ["9" "436248" "0" "5.331040" "40495344" "strcmp" "0" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "951" "2" "string(10)" "string(7)"] ["9" "436248" "1" "5.331059" "40495344"] ["9" "436249" "0" "5.331065" "40495216" "HuddlerObject->isProperty" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "954" "1" "string(10)"] ["10" "436250" "0" "5.331076" "40495344" "strchr" "0" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "969" "2" "string(10)" "string(1)"] ["10" "436250" "1" "5.331093" "40495344"] ["10" "436251" "0" "5.331099" "40495216" "HuddlerObject->hasColumn" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "972" "1" "string(10)"] ["11" "436252" "0" "5.331111" "40495216" "Columns::has_column" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "460" "2" "string(13)" "string(10)"] ["11" "436252" "1" "5.331133" "40495264"] ["10" "436251" "1" "5.331146" "40495264"] ["10" "436253" "0" "5.331152" "40495216" "HuddlerObject->hasAlias" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "975" "1" "string(10)"] ["10" "436253" "1" "5.331170" "40495264"] ["9" "436249" "1" "5.331184" "40495264"] ["9" "436254" "0" "5.331189" "40495216" "HuddlerObject->getProperty" "1" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "955" "1" "string(10)"] ["10" "436255" "0" "5.331201" "40495344" "strchr" "0" "" "/ssd1/var/www/html/logan/v2/system/application/libraries/objects/HuddlerObject.php" "1003" "2" "string(10)" "string(1)"] ["10" "436255" "1" "5.331217" "40495344"]))

(defn make-root [] [:root []])

(defn line->child [line]
  [{:fn-name (l/fn-name line)
    :fn-num (l/fn-num line)
    :time [(l/time line)]
    :memory [(l/memory line)]
    :user-defined? (l/user-defined? line)
    :file (l/file line)
    :line-num (l/line-num line)}
   []])

(defn update-child [node line]
  (-> node
      (update-in [0 :time] assoc 1 (l/time line))
      (update-in [0 :memory] assoc 1 (l/memory line))))

(defn node-data [node] (first node))

(defn node-children [node] (seq (second node)))

(defn make-node [node children] (with-meta [(node-data node) (vec children)] (meta node)))

(defn zipper
  ([] (zipper (make-root)))
  ([root] (zip/zipper (constantly true) node-children make-node root)))

(defn expected-exit? [node line] (= (-> node node-data :fn-num) (l/fn-num line)))

(defn enter-fn [loc line]
  (-> loc
      (zip/append-child (line->child line))
      zip/down
      zip/rightmost))

(defn exit-fn [loc line]
  (-> loc
      (zip/edit update-child line)
      zip/up))

(defn accept-line
  "Collects a line into zipper structure at current location and returns it.
  If it's an entry line, we push a level to stack, otherwise collect exiting
  time/memory and pop stack."
  [loc line]
  (cond
    (l/entry-line? line)
    (enter-fn loc line)

    (expected-exit? (zip/node loc) line)
    (exit-fn loc line)

    :else (throw (RuntimeException. (str "Unmatching child: " line)))))

(defn read-trace [lines]
  (let [z (zipper)]
    (-> (reduce accept-line z lines)
        zip/root)))

(defn trace-line-seq
  "Returns lazy sequence of lines "
  [^Reader rdr]
  (->> (line-seq rdr)
       (map #(str/split % #"\t"))
       (drop-while #(or (not (l/line? %)) (not (l/entry-line? %))))))

(defn -main [& [path]]
  (with-open [rdr (io/reader path)]
    (let [lines (trace-line-seq rdr)]
      (pprint (read-trace (take 100 lines))))))
