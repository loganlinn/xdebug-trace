(ns xdebug-trace.main
  (:gen-class :main :true)
  (:require [xdebug-trace.system :as system]))

(defn -main
  [& args]
  (-> (system/system)
      (system/start))
