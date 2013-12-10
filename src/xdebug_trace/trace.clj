(ns xdebug-trace.trace
  "Trace information and statistics"
  (:require [xdebug-trace.util :refer [deep-merge-with]]
            [clojure.core.reducers :as r]
            [clojure.data.priority-map :refer [priority-map]])
  (:import [java.io File]))

(defn- merge+
  ([] {})
  ([m1 m2] (deep-merge-with + m1 m2))
  ([m1 m2 m3] (deep-merge-with + m1 m2 m3)))

(defn top-n-by
  "Returns a function that accepts a priority-map, a record/map, and a property,
  and returns the prioirity map including the record/map if value of property is
  within top-n. Returned function is meant to be suitable for reduce"
  ([n] (top-n-by compare n))
  ([cmp n]
   (fn [pm rec prop]
     (if-let [v (prop rec)]
       (let [[cmp-rec cmp-v] (peek pm)]
         (cond
           (or (nil? cmp-rec) (< (count pm) n))
           (conj pm [rec v])

           (< (cmp cmp-v v) 0)
           (conj (pop pm) [rec v])

           :else pm))
       pm))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Records

(defrecord Trace [time stack file])

(defrecord TraceFunction
  [fn-name
   fn-num
   level
   time
   memory
   user-defined?
   file
   line-num
   arguments])

(defn flatten-summary-fns
  "Updates the :fns map (fn-name to stats) to be a sequence of maps with fn-name
  associated inside"
  [{fns :fns :as summary}]
  (assoc summary :fns (map #(assoc (val %) :fn-name (key %)) fns)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocols

;; TODO
;(defprotocol TimedEvent
  ;(duration [this]))

(defprotocol NamedTrace (trace-name [this]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(def trace-ext ".xt")

(defn start-val [interval] (nth interval 0))

(defn end-val [interval] (nth interval 1))

(defn delta
  [[start end]] (if end (- end start) 0))

(defn memory-delta [{[start end] :memory}]
  (when end (- end start)))

(defn time-delta [{[start end] :time}]
  (when end (- end start)))

(defn stack-time-min [stack]
  (first (:time (ffirst stack))))

(defn stack-time-max [stack]
  (let [[{[start end] :time :as trace} fns] (last stack)]
    (cond
      end end
      (empty? fns) start
      :else (recur fns))))

(defn stack-time-range [stack]
  (- (stack-time-max stack) (stack-time-min stack)))

(defn fn-summary [trace fn-name]
  (->> (:stack trace)
       (flatten)
       (r/filter #(= fn-name (:fn-name %)))
       (r/map #(-> (select-keys % [:time :memory])
                   (update-in [:time] delta)
                   (update-in [:memory] delta)
                   (assoc :n 1)))
       (r/fold merge+)))

(defn trace-summary [trace]
  (let [inc* (fnil inc 0)]
    (->> (:stack trace)
         (flatten)
         (r/fold
           merge+
           (fn [memo {:keys [fn-name time memory]}]
             (let [fn-stats {:time (delta time)
                             :memory (delta memory)
                             :n 1}]
               ;; increment global & fn specific counters
               (merge+ memo
                       fn-stats
                       {:fns {fn-name fn-stats}}))))
         (flatten-summary-fns))))

;; TODO add max-level
(defn trace-summary-top-n [n trace-summary]
  (let [top-fn (top-n-by n)
        rev-keys (comp reverse keys)]
    (-> (reduce
          (fn [stats rec]
            (-> stats
                (update-in [:time] top-fn rec :time)
                (update-in [:memory] top-fn rec :memory)
                (update-in [:n] top-fn rec :n)))
          {:time (priority-map)
           :memory (priority-map)
           :n (priority-map)}
          (:fns trace-summary))
        (update-in [:time] rev-keys)
        (update-in [:memory] rev-keys)
        (update-in [:n] rev-keys))))

(defn sort-fn-traces
  "Sort functions in trace-summary by property"
  [summary prop]
  {:pre [(#{:time :memory :n} prop)]}
  (update-in summary [:fns] #(sort-by (comp prop second) > %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Protocol Ext

(extend-protocol NamedTrace
  File
  (trace-name [this]
    (let [filename (.getName this)]
      (if (.endsWith filename trace-ext)
        (subs filename 0 (- (.length filename) (.length trace-ext)))
        filename)))
  Trace
  (trace-name [this]
    (when-let [^File f (:file this)]
      (trace-name f))))

