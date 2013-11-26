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
  ;{:pre [(expected-exit? loc line)]}
  (-> loc
      (zip/edit exit-node line)
      zip/up))

(defn accept-line
  "Collects a line into zipper structure at current location and returns it.
  If it's an entry line, we push a level to stack, otherwise collect exiting
  time/memory and pop stack."
  [loc line]
  (if (l/line? line)
    (cond
      (l/entry-line? line) (enter-fn loc line)
      (expected-exit? loc line) (exit-fn loc line)
      :else loc)
    (cond
      (l/trace-start? line)
      (with-meta loc (assoc (meta loc) ::start (l/date line)))
      (l/trace-end? line)
      (with-meta loc (assoc (meta loc) ::end (l/date line)))
      :else loc)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn read-trace [lines]
  (let [root-node (make-root)
        loc (zipper root-node)
        trace (reduce accept-line loc lines)
        trace-meta (meta trace)]
    (with-meta
      (-> trace zip/root node-children)
      (select-keys trace-meta [::start ::end]))))

(comment
 (defn lazy-read-trace
  [[first-line & rest-lines]]
  (let [level (l/level first-line)
        [now later] (split-with #(not= (l/level %) level) rest-lines)]
    (lazy-cat
      (read-trace (cons first-line now))
      (lazy-read-trace (take 10 later))))))

(defn trace-line-seq
  "Returns lazy sequence of lines "
  [^Reader rdr]
  (map #(str/split % #"\t") (line-seq rdr)))
