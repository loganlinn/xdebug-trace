(ns xdebug-trace.view.trace
  "Renders HTML for viewing trace"
  (:require [xdebug-trace.view.layout :refer [defpage]]
            [xdebug-trace.view.util :refer [filepath->repo-url]]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [hiccup.page :as page]))

(def intial-collapse-depth 1)
(def max-depth 8)

(defn time-label [[start end]]
  (if-not end
    "?"
    (let [diff (* 1000 (- end start))]
      (format "%.2f ms" (float diff)))))

(defn time-class [[start end]]
  (when end
    (let [diff (* 1000 (- end start))]
      (cond
        (> diff 10.) "badge-important"
        (> diff 0.1) "badge-info"))))

(defn mem-label [[start end]]
  (if-not end
    "?"
    (let [diff (- end start)]
      (cond
        (= end start) "0"
        (> diff 0) (str "+" diff)
        :else diff))))

(defn mem-class [[start end]]
  (when end
    (let [diff (- end start)]
      (cond
        (= diff 0) nil
        (> diff 128) "badge-important"
        (> diff 0) "badge-warning"
        :else "badge-success"))))

(declare render-trace-fns)

(defn render-trace-fn
  [[{:keys [fn-name fn-num time memory file line-num depth arguments] :as trace} sub-traces]]
  (if (< depth max-depth)
    (let [collapse-id (str "collapse_" fn-num)]
      [:div.accordion-group
       {:data-depth depth}
       [:div.accordion-heading
        [:a.accordion-toggle
         {:data-toggle "collapse"
          :href (str "#" collapse-id)}
         [:h4.fn-name fn-name
          [:span.time-diff.badge.pull-right
           {:class (time-class time)} (time-label time)]
          [:span.memory-diff.badge.pull-right
           {:class (mem-class memory)} (mem-label memory)]]]]
       [:div.accordion-body.collapse
        {:id collapse-id
         :class (if (< depth intial-collapse-depth) "in")}
        [:div.accordion-inner
         [:div
          [:code.location.muted
           (let [loc (str file ":" line-num)]
             (if-let [url (filepath->repo-url file line-num)]
               [:a {:href url :target "_blank"} loc]
               loc))]]
         [:ul.inline
          (for [arg arguments]
            [:li [:code.argument.muted arg]])]
         (if (seq sub-traces)
           (render-trace-fns sub-traces))]]])))

(defn render-trace-fns [trace]
  [:div.trace.accordion (map render-trace-fn trace)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn trace-url [trace-name] (str "/trace/" trace-name))

(defpage render-trace [trace]
  (defblock head-end
    (page/include-css "/css/trace.css"))
  (defblock content
    [:div.row
     [:div.span12
      [:h1 "Trace"]
      (render-trace-fns trace)]]))

;; TODO Don't take Files
(defpage render-traces [trace-files]
  (defblock content
    [:div.row
     [:div.span12
      [:h1 "Available Traces"]
      [:table.table.table-hover.table-bordered
       [:thead
        [:tr
         [:td "Name"]
         [:td "Last Modified"]]]
       [:tbody
        (for [^java.io.File f trace-files]
          (let [filename (.getName f)
                trace-name (.substring filename 0 (.lastIndexOf filename "."))
                last-modified (->> (.lastModified f)
                                   (tc/from-long)
                                   (tf/unparse (tf/formatters :rfc822)))]
            [:tr
             [:td [:a {:href (trace-url trace-name)} trace-name]]
             [:td last-modified]]))]]]]))
