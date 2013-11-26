(ns xdebug-trace.trace
  "Trace information and statistics"
  (:require [clojure.core.reducers :as r]
            [clojure.data.priority-map :refer [priority-map]]))

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& vs]
      (if (every? map? vs)
        (apply merge-with m vs)
        (apply f vs)))
    maps))

(defn merge+
  ([] {})
  ([m1 m2] (deep-merge-with + m1 m2))
  ([m1 m2 m3] (deep-merge-with + m1 m2 m3)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Records

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn delta
  [[start end]] (if end (- end start) 0))

(defn memory-delta [{[start end] :memory}]
  (when end (- end start)))

(defn time-delta [{[start end] :time}]
  (when end (- end start)))

(defn fn-summary [trace fn-name]
  (->> (flatten trace)
       (r/filter #(= fn-name (:fn-name %)))
       (r/map #(-> (select-keys % [:time :memory])
                   (update-in [:time] delta)
                   (update-in [:memory] delta)
                   (assoc :n 1)))
       (r/fold merge+)))

(defn trace-summary [trace]
  (let [inc* (fnil inc 0)]
    (->> (flatten trace)
         (r/fold
           merge+
           (fn [memo {:keys [fn-name time memory]}]
             (let [fn-stats {:time (delta time)
                             :memory (delta memory)
                             :n 1}]
               ;; increment global & fn specific counters
               (merge+ memo
                       fn-stats
                       {:fns {fn-name fn-stats}})))))))

(defn top-n-by
  ([n] (top-n-by compare n))
  ([comp n]
   (fn [pm rec prop]
     (if-let [v (prop rec)]
       (let [[comp-rec comp-v] (peek pm)]
         (cond
           (or (nil? comp-rec) (< (count pm) n))
           (conj pm [rec v])

           (< (comp comp-v v) 0)
           (conj (pop pm) [rec v])

           :else pm))
       pm))))

(defn trace-summary-top-n [n trace-summary]
  (let [top-fn (top-n-by n)]
    (reduce
      (fn [stats rec]
        (-> stats
            (update-in [:time] top-fn rec :time)
            (update-in [:memory] top-fn rec :memory)
            (update-in [:n] top-fn rec :n)))
      {:time (priority-map)
       :memory (priority-map)
       :n (priority-map)}
      (r/map #(assoc (val %) :fn-name (key %)) (:fns trace-summary)))))

(defn sort-fn-traces
  "Sort functions in trace-summary by property"
  [summary prop]
  {:pre [(#{:time :memory :n} prop)]}
  (update-in summary [:fns] #(sort-by (comp prop second) > %)))
