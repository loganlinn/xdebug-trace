(ns xdebug-trace.trace
  "Trace information and statistics")

(defn- diff [[start end]] (if end (- end start) 0))

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

(defn fn-summary [trace fn-name]
  (reduce
    (fn [stats {:keys [time memory arguments]}]
      (-> stats
          (assoc :n (inc (:n stats)))
          (assoc :time (+ (:time stats) (diff time)))
          (assoc :memory (+ (:memory stats) (diff memory)))))
    {:n 0
     :time 0
     :memory 0}
    (filter #(= fn-name (:fn-name %)) (flatten trace))))
