(ns xdebug-trace.view.index
  (:require [xdebug-trace.view.layout :refer [defpage]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defpage index []
  (defblock content
    [:div.hero-unit
     [:h1 "Xdebug Trace Tool"]
     [:a.btn.btn-primary.btn-large
      {:href "/trace"} "View Available Traces"]]))
