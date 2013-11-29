(ns xdebug-trace.view.trace-summary
  (:require [xdebug-trace.view.layout :refer [defpage]]
            [xdebug-trace.view.util :refer [merged-query-str]]
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

(defn n-menu [current-n]
  [:div.btn-group
   [:button.btn.btn-default.dropdown-toggle
    {:type "button"
     :data-toggle "dropdown"}
    "Result Size " [:span.caret]]
   [:ul.dropdown-menu
    (for [n [5 10 25 50]
          :let [current? (= n current-n)]]
      [:li
       {:class (if current? "active")}
       [:a {:href (str "?" (merged-query-str {:n n}))} n]])]])


(defpage trace-summary [trace-name summary n]
  (defblock content
    [:div.row
     [:div.col-xs-12
      [:div.pull-right (n-menu n)]
      [:h1 "Trace Summary"]
      [:h3 "Calls"]
      (trace-table (:n summary))
      [:h3 "Time"]
      (trace-table (:time summary))
      [:h3 "Memory"]
      (trace-table (:memory summary))]]))
