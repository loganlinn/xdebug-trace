(ns xdebug-trace.view.trace
  (:require [xdebug-trace.view.layout :refer [defpage]]
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
        (<= diff 1.) nil
        :else "badge-info"))))

(defn mem-label [[start end]]
  (if-not end
    "?"
    (let [diff (- end start)]
      (cond
        (= end start) "0"
        (> end start) (str "+" (- end start))
        :else (str "-" (- end start))))))

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
          [:span.time-diff.badge
           {:class (time-class time)} (time-label time)]
          [:span.memory-diff.badge
           {:class (mem-class memory)} (mem-label memory)]]]]
       [:div.accordion-body.collapse
        {:id collapse-id
         :class (if (< depth intial-collapse-depth) "in")}
        [:div.accordion-inner
         [:code.location.muted [:span.file file] ":" [:span.line-num line-num]]
         (map (fn [arg] [:code.argument.muted arg]) arguments)
         (if (seq sub-traces)
           (render-trace-fns sub-traces))]]])))

(defn render-trace-fns [trace]
  [:div.trace.accordion (map render-trace-fn trace)])

(defpage render-trace [trace]
  (defblock head-end
    (page/include-css "/css/trace.css"))
  (defblock content
    [:div.row
     [:div.span12
      (render-trace-fns trace)]]))
