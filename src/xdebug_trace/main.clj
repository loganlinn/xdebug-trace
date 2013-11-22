(ns xdebug-trace.main
  (:gen-class)
  (:require [xdebug-trace.system :as system]))

(defn -main
  [& args]
  (-> (system/system)
      (system/start)))
