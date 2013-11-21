(ns xdebug-trace.system
  (:require [xdebug-trace.service :as service]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn system []
  {:server nil
   :port (Integer/parseInt (:port env "8080"))})

(defn start [system]
  (assoc system :server
         (run-jetty (service/create-app)
                    {:join? false :port (:port system)})))

(defn stop [system]
  (when-let [server (:server system)]
    (.stop server))
  (dissoc system :server))

