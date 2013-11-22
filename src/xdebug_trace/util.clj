(ns xdebug-trace.util)

(defn distinct-on [f coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                 ((fn [[x :as xs] seen]
                    (when-let [s (seq xs)]
                      (let [xv (f x)]
                        (if (contains? seen xv)
                          (recur (rest s) seen)
                          (cons x (step (rest s) (conj seen xv)))))))
                  xs seen)))]
    (step coll #{})))
