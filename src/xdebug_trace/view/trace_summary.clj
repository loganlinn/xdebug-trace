(ns xdebug-trace.view.trace-summary
  (:require [xdebug-trace.view.layout :refer [defpage]]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [hiccup.page :as page]))

(defn trace-table [fns]
  [:table.table.table-striped.table-hover
   [:thead
    [:tr
     [:th "#"]
     [:th "Function"]
     [:th "Memory (total)"]
     [:th "Time (total)"]
     [:th "Calls"]]]
   [:tbody
    (map-indexed
      (fn [index {:keys [fn-name memory time n]}]
        [:tr
         [:td (inc index)]
         [:td fn-name]
         [:td (format "%,(.0f kb" (/ memory 1024.))]
         [:td (format "%,.2f ms" time)]
         [:td n]])
      fns)]])

(defpage trace-summary [trace-name summary n]
  (defblock content
    [:div.row
     [:div.col-xs-12
      [:h1 "Trace Summary"]
      [:h3 "Calls"]
      (trace-table (:n summary))
      [:h3 "Time"]
      (trace-table (:time summary))
      [:h3 "Memory"]
      (trace-table (:memory summary))]]))
