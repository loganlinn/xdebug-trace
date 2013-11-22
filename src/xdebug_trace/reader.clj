(ns xdebug-trace.reader
  "Reads Xdebug trace files into Clojure data structure
  Traces are a nested data structure of the form: [fn-info sub-traces]
  where sub-traces is a sequence of traces"
  (:require [xdebug-trace.line :as l]
            [xdebug-trace.trace :as trace]
            [clojure.pprint :refer [pprint]]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import java.io.Reader))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Zipper Constructs

(defn make-root [] [{:root true} []])

;; node: [value children]
;; children: [node ...]
(defn line->node [line]
  [(trace/->TraceFunction
     (l/fn-name line)
     (l/fn-num line)
     (l/level line)
     [(l/time line) nil]
     [(l/memory line) nil]
     (l/user-defined? line)
     (l/file line)
     (l/line-num line)
     (l/arguments line))
   []])

(defn exit-node [node line]
  (-> node
      (update-in [0 :time] assoc 1 (l/time line))
      (update-in [0 :memory] assoc 1 (l/memory line))))

(defn node-value [node] (first node))

(defn node-children [node] (seq (second node)))

(defn make-node [node children] (with-meta [(node-value node) (vec children)] (meta node)))

(defn zipper
  ([] (zipper (make-root)))
  ([root] (zip/zipper (constantly true) node-children make-node root)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Zipper Operations

(defn assoc-depth [loc depth]
  (zip/edit loc assoc-in [0 :depth] depth))

(defn enter-fn [loc line]
  (let [depth (-> loc zip/node node-value (:depth 0))]
    (-> loc
        (zip/append-child (line->node line))
        zip/down
        zip/rightmost
        (assoc-depth (inc depth)))))

(defn expected-exit? [loc line]
  (= (-> loc zip/node node-value :fn-num) (l/fn-num line)))

(defn exit-fn
  [loc line]
  {:pre [(expected-exit? loc line)]}
  (-> loc
      (zip/edit exit-node line)
      zip/up))

(defn accept-line
  "Collects a line into zipper structure at current location and returns it.
  If it's an entry line, we push a level to stack, otherwise collect exiting
  time/memory and pop stack."
  [loc line]
  (cond
    (not (l/line? line)) loc
    (l/entry-line? line) (enter-fn loc line)
    :else (exit-fn loc line)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn read-trace [lines]
  (let [root-node (make-root)
        loc (zipper root-node)]
    (-> (reduce accept-line loc lines)
        zip/root
        node-children)))

(defn trace-line-seq
  "Returns lazy sequence of lines "
  [^Reader rdr]
  (->> (line-seq rdr)
       (map #(str/split % #"\t"))
       (drop-while #(or (not (l/line? %)) (not (l/entry-line? %))))))
