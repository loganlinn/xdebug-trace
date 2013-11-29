(ns xdebug-trace.view.trace
  "Renders HTML for viewing trace"
  (:require [xdebug-trace.view.layout :refer [defpage]]
            [xdebug-trace.view.util :refer [filepath->repo-url]]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [hiccup.page :as page]))

(def intial-collapse-depth 1)
(def default-max-depth 8)

(defn time-label [[start end]]
  (if-not end "?"
    (format "%.2f ms" (- end start))))

(defn time-class [[start end]]
  (when end
    (let [diff (- end start)]
      (cond
        (> diff 10.) "label-warning"
        (> diff 0.1) "label-info"
        :else "label-default"))))

(defn mem-label [[start end]]
  (if-not end "?"
    (format "%+,d bytes" (- end start))))

(defn mem-class [[start end]]
  (when end
    (let [diff (- end start)]
      (cond
        (= diff 0) "label-default"
        (> diff 128) "label-danger"
        (> diff 0) "label-info"
        :else "label-success"))))

(declare render-trace-fns)

(defn render-trace-fn
  [[trace sub-traces] max-depth]
  (let [{:keys [fn-name fn-num time memory file line-num depth arguments]} trace
        collapse-id (str "collapse_" fn-num)]
    [:div.panel.panel-default
     {:data-depth depth}
     [:div.panel-heading
      [:h4.panel-title
       [:a {:data-toggle "collapse" :href (str "#" collapse-id)}
        [:span.time-diff.label.pull-right
         {:class (time-class time)} (time-label time)]
        [:span.memory-diff.label.pull-right
         {:class (mem-class memory)} (mem-label memory)]
        fn-name ]]]
     [:div.panel-collapse.collapse
      {:id collapse-id
       :class (if (< depth intial-collapse-depth) "in")}
      [:div.panel-body
       [:div
        [:code.location.muted
         (let [loc (str file ":" line-num)]
           (if-let [url (filepath->repo-url file line-num)]
             [:a {:href url :target "_blank"} loc]
             loc))]]
       [:ul.inline
        (for [arg arguments]
          [:li [:code.argument.muted arg]])]
       (if (or (not max-depth) (< depth max-depth))
         (render-trace-fns sub-traces max-depth))]]]))

(defn render-trace-fns
  [trace max-depth]
  (when trace
    [:div.trace.accordion
     (for [tf trace] (render-trace-fn tf max-depth))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn trace-url [trace-name] (str "/trace/" trace-name))
(defn trace-summary-url [trace-name] (str "/trace/" trace-name "/summary"))

(defn trace-nav
  ([trace-name] (trace-nav trace-name nil))
  ([trace-name current]
   (let [tabs {:view ["View" (trace-url trace-name)]
               :summary ["Summary" (trace-summary-url trace-name)]}]
     [:ul.nav.nav-tabs.trace-nav
      (for [[tab [label url]] tabs]
        [:li {:class (if (= current tab) "active")}
         [:a {:href url} label]])])))

(defpage render-trace [trace-name trace & {:keys [max-depth]}]
  (defblock head-end
    (page/include-css "/css/trace.css"))
  (defblock content
    [:div.row
     [:div.span12
      (trace-nav trace-name :view)
      (render-trace-fns trace (or max-depth default-max-depth))]]))

;; TODO Don't take Files
(defpage list-traces [trace-files]
  (defblock content
    [:div.row
     [:div.col-xs-12
      [:h1 "Available Traces"]
      [:table.table.table-hover.table-bordered
       [:thead
        [:tr
         [:td "Trace"]
         [:td "Actions"]
         [:td "Last Modified"]]]
       [:tbody
        (for [^java.io.File f trace-files]
          (let [filename (.getName f)
                trace-name (.substring filename 0 (.lastIndexOf filename "."))
                last-modified (->> (.lastModified f)
                                   (tc/from-long)
                                   (tf/unparse (tf/formatters :rfc822)))
                view-url (trace-url trace-name)
                summary-url (trace-summary-url trace-name)]
            [:tr
             [:td [:a {:href view-url} trace-name]]
             [:td
              [:div.btn-group
               [:a.btn.btn-default {:href view-url} "View"]
               [:a.btn.btn-default {:href summary-url} "Summary"]]]
             [:td last-modified]]))]]]]))
