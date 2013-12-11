(ns xdebug-trace.view.trace
  "Renders HTML for viewing trace"
  (:require [xdebug-trace.trace :as trace]
            [xdebug-trace.view.layout :refer [defpage]]
            [xdebug-trace.view.util :refer [filepath->repo-url
                                            css-bar-chart]]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [hiccup.page :as page]))

(def intial-collapse-depth 1)
(def default-max-depth 8)

(defn time-badge [{[start end] :time est? :time-estimated?}]
  (let [diff (when end (- end start))
        c (cond (not diff) nil
                (> diff 10.) "label-warning"
                (> diff 0.1) "label-info"
                :else "label-default")]
    [:span.time-diff.label.pull-right
     {:class c}
     (if diff
       (format (str (if est? "> ") "%.2f ms") diff)
       "?")]))

(defn mem-badge [{[start end] :memory est? :memory-estimated?}]
  (when end
    (let [diff (- end start)
          c (cond
              (= diff 0) "label-default"
              (> diff 128) "label-danger"
              (> diff 0) "label-info"
              :else "label-success")]
      [:span.memory-diff.label.pull-right
       {:class c}
       (format (str (if est? "> ") "%+,d bytes") diff)])))

(declare render-trace-stack)

(defn render-trace-fn
  [parent-fn [tfn sub-traces] time-bar-chart max-depth]
  (let [{:keys [fn-name fn-num time memory file line-num depth arguments]} tfn
        t-diff (trace/delta time)
        m-diff (trace/delta memory)
        significant? (and parent-fn (trace/significant-child? parent-fn tfn))
        collapse-id (str "collapse_" fn-num)]
    [:div.panel
     {:data-depth depth
      :class (if significant? "panel-info" "panel-default")}
     [:div.panel-heading
      [:h4.panel-title
       [:a {:data-toggle "collapse" :href (str "#" collapse-id)}
        (time-badge tfn)
        (mem-badge tfn)
        fn-name
        ;(time-bar-chart time)
        ]]]
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
         (render-trace-stack tfn sub-traces time-bar-chart max-depth))]]]))

(defn render-trace-stack
  ([stack time-bar-chart max-depth]
   (render-trace-stack nil stack time-bar-chart max-depth ))
  ([parent-fn stack time-bar-chart max-depth]
   (when stack
     [:div.trace.accordion
      (for [tfn stack]
        (render-trace-fn parent-fn tfn time-bar-chart max-depth))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public

(defn trace-url [trace-name] (str "/trace/" trace-name))
(defn trace-summary-url [trace-name] (str "/trace/" trace-name "/summary"))

(defn trace-nav
  [trace current-tab]
  (let [trace-name (trace/trace-name trace)
        tabs {:view ["View" (trace-url trace-name)]
              :summary ["Summary" (trace-summary-url trace-name)]}]
    [:ul.nav.nav-tabs.trace-nav
     (for [[tab [label url]] tabs]
       [:li {:class (if (= current-tab tab) "active")}
        [:a {:href url} label]])]))

(defn trace-header [trace time-total]
  [:div
   [:h3 (trace/trace-name trace)]
   [:h4 (format "Total Time %,f ms" time-total)]])

(defpage render-trace [{:keys [time memory stack] :as trace} {:keys [max-depth]}]
  (defblock head-end
    (page/include-css "/css/trace.css"))
  (defblock content
    (let [time-start (trace/start-val (:time trace))
          time-end   (trace/end-val (:time trace))
          time-total (- time-end time-start)]
      [:div.row
       [:div.span12
        (trace-header trace time-total)
        (trace-nav trace :view)
        (render-trace-stack
          trace
          stack
          #(css-bar-chart (/ (trace/delta %) time-total)
                          (/ (- (trace/start-val %) time-start) time-total))
          (or max-depth default-max-depth))]])))

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
          (let [trace-name (trace/trace-name f)
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
