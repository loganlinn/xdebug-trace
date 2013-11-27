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

(defn deep-merge-with [f & maps]
  (apply
    (fn m [& vs]
      (if (every? map? vs)
        (apply merge-with m vs)
        (apply f vs)))
    maps))
