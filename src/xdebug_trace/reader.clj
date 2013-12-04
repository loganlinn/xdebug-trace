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
  (:import [java.io Reader File]))

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

(defn unzip [loc] (zip/root loc))

(defn node-assoc [loc k v]
  (zip/edit loc assoc-in [0 k] v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Zipper Operations

(defn topmost
  "Returns loc of topmost (root) node"
  [loc]
  (if (zip/end? loc)
    loc
    (let [p (zip/up loc)]
      (if p
        (recur p)
        loc))))

(defn enter-fn [loc line]
  (let [depth (-> loc zip/node node-value (:depth 0))]
    (-> loc
        (zip/append-child (line->node line))
        zip/down
        zip/rightmost
        (node-assoc :depth (inc depth)))))

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
      (-> (topmost loc)
          (node-assoc :start (.getTime (l/date line))))
      (l/trace-end? line)
      (-> (topmost loc)
          (node-assoc :end (.getTime (l/date line))))
      :else loc)))

(defn- create-trace [file root-node]
  (let [root (node-value root-node)
        stack (node-children root-node)]
    (trace/map->Trace {:time [(:start root) (:end root)]
                       :stack stack
                       :file file})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn read-trace-lines [lines]
  (let [build-trace #(reduce accept-line % lines)]
    (-> (zipper) (build-trace) (unzip))))

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

(defn read-trace-file
  ([^File file] (read-trace-file file nil nil nil))
  ([^File file limit offset max-depth]
   (with-open [rdr (io/reader file)]
     (cond->> (trace-line-seq rdr)
       offset (drop offset)
       limit  (take limit)
       ;; TODO max-depth
       true   (read-trace-lines)
       true   (create-trace file)))))
