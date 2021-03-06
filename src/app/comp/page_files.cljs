
(ns app.comp.page-files
  (:require [clojure.string :as string]
            [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo-ui.colors :as colors]
            [respo.core :refer [defcomp cursor-> list-> <> span div pre input button a]]
            [respo.comp.inspect :refer [comp-inspect]]
            [respo.comp.space :refer [=<]]
            [app.style :as style]
            [app.comp.changed-files :refer [comp-changed-files]]
            [keycode.core :as keycode]
            [app.comp.file-replacer :refer [comp-file-replacer]]
            [app.util.shortcuts :refer [on-window-keydown]]
            [respo-alerts.comp.alerts :refer [comp-prompt]]))

(defn on-edit-def [text] (fn [e d! m!] (d! :writer/edit {:kind :def, :extra text})))

(defn on-edit-ns [e d! m!] (d! :writer/edit {:kind :ns}))

(defn on-edit-proc [e d! m!] (d! :writer/edit {:kind :proc}))

(defn on-input-def [state] (fn [e d! m!] (m! (assoc state :def-text (:value e)))))

(defn on-keydown-def [state]
  (fn [e d! m!]
    (let [text (string/trim (:def-text state)), code (:key-code e)]
      (if (and (= code keycode/return) (not (string/blank? text)))
        (do (d! :ir/add-def text) (m! (assoc state :def-text "")))
        (on-window-keydown (:event e) d! {:name :files})))))

(defn on-remove-def [def-text] (fn [e d! m!] (d! :ir/remove-def def-text)))

(def style-def {:padding "0 8px", :position :relative, :color (hsl 0 0 80)})

(def style-file {:width 280, :overflow :auto, :padding-bottom 120})

(def style-input (merge style/input {:width "100%"}))

(def style-link {:cursor :pointer})

(def style-remove
  {:color (hsl 0 0 80),
   :cursor :pointer,
   :vertical-align :middle,
   :position :absolute,
   :top 8,
   :right 8})

(defcomp
 comp-file
 (states selected-ns defs-set highlights)
 (let [state (or (:data states) {:def-text ""})]
   (div
    {:style style-file}
    (div
     {}
     (<> "File" style/title)
     (=< 16 nil)
     (span
      {:inner-text "Draft",
       :style style/button,
       :on {:click (fn [e d! m!] (d! :writer/draft-ns selected-ns))}})
     (cursor->
      :duplicate
      comp-prompt
      states
      {:trigger (span {:inner-text "Clone", :style style/button}),
       :initial selected-ns,
       :text "A new namespace:"}
      (fn [result d! m!]
        (if (string/includes? result ".")
          (d! :ir/clone-ns result)
          (d! :notify/push-message [:warn (str "Not a good name: " result)])))))
    (div
     {}
     (span {:inner-text selected-ns, :style style-link, :on {:click on-edit-ns}})
     (=< 16 nil)
     (span {:inner-text "proc", :style style-link, :on {:click on-edit-proc}})
     (=< 16 nil))
    (div
     {}
     (input
      {:value (:def-text state),
       :placeholder "a def",
       :style style-input,
       :on {:input (on-input-def state), :keydown (on-keydown-def state)}}))
    (=< nil 8)
    (list->
     :div
     {}
     (->> defs-set
          (filter (fn [def-text] (string/includes? def-text (:def-text state))))
          (sort)
          (map
           (fn [def-text]
             [def-text
              (div
               {:class-name "hoverable",
                :style (merge
                        style-def
                        (if (contains?
                             highlights
                             {:ns selected-ns, :extra def-text, :kind :def})
                          {:color :white})),
                :on {:click (on-edit-def def-text)}}
               (<> span def-text nil)
               (=< 16 nil)
               (span
                {:class-name "ion-trash-b is-minor",
                 :title "Remove def",
                 :style style-remove,
                 :on {:click (on-remove-def def-text)}}))])))))))

(defn on-checkout [state ns-text] (fn [e d! m!] (d! :session/select-ns ns-text)))

(defn on-input-ns [state] (fn [e d! m!] (m! (assoc state :ns-text (:value e)))))

(defn on-keydown-ns [state]
  (fn [e d! m!]
    (let [text (string/trim (:ns-text state)), code (:key-code e)]
      (if (and (= code keycode/return) (not (string/blank? text)))
        (cond
          (string/starts-with? text "mv ")
            (let [[_ from to] (string/split text " ")]
              (d! :ir/mv-ns {:from from, :to to})
              (m! (assoc state :ns-text "")))
          (string/starts-with? text "cp ")
            (let [[_ from to] (string/split text " ")]
              (d! :ir/cp-ns {:from from, :to to})
              (m! (assoc state :ns-text "")))
          :else (do (d! :ir/add-ns text) (m! (assoc state :ns-text ""))))
        (on-window-keydown (:event e) d! {:name :files})))))

(defn on-remove-ns [ns-text] (fn [e d! m!] (d! :ir/remove-ns ns-text)))

(def style-empty {:width 280})

(defn render-empty [] (div {:style style-empty} (<> span "Empty" nil)))

(def style-list {:width 280, :overflow :auto, :padding-bottom 120})

(def style-ns
  {:cursor :pointer,
   :vertical-align :middle,
   :position :relative,
   :padding "0 8px",
   :color (hsl 0 0 80)})

(defn render-list [state ns-set selected-ns ns-highlights]
  (div
   {:style style-list}
   (div {:style style/title} (<> span "Namespaces" nil))
   (div
    {}
    (input
     {:value (:ns-text state),
      :placeholder "a namespace",
      :style style-input,
      :on {:input (on-input-ns state), :keydown (on-keydown-ns state)}}))
   (=< nil 8)
   (list->
    :div
    {}
    (->> ns-set
         (filter (fn [ns-text] (string/includes? ns-text (:ns-text state))))
         (sort)
         (map
          (fn [ns-text]
            [ns-text
             (div
              {:class-name (if (= selected-ns ns-text) "hoverable is-selected" "hoverable"),
               :style (merge
                       style-ns
                       (if (contains? ns-highlights ns-text) {:color :white})),
               :on {:click (on-checkout state ns-text)}}
              (span {:inner-text ns-text})
              (span
               {:class-name "ion-trash-b is-minor",
                :title "Remove ns",
                :style style-remove,
                :on {:click (on-remove-ns ns-text)}}))]))))))

(def style-inspect {:opacity 1, :background-color (hsl 0 0 100), :color :black})

(def sytle-container {:padding "0 16px"})

(defcomp
 comp-page-files
 (states selected-ns router-data)
 (let [state (or (:data states) {:ns-text ""})
       highlights (set (map last (:highlights router-data)))
       ns-highlights (set (map :ns highlights))]
   (div
    {:style (merge ui/flex ui/row sytle-container)}
    (render-list state (:ns-set router-data) selected-ns ns-highlights)
    (=< 32 nil)
    (if (some? selected-ns)
      (cursor-> selected-ns comp-file states selected-ns (:defs-set router-data) highlights)
      (render-empty))
    (=< 32 nil)
    (cursor-> :files comp-changed-files states (:changed-files router-data))
    (comment comp-inspect selected-ns router-data style-inspect)
    (if (some? (:peeking-file router-data))
      (cursor-> :replacer comp-file-replacer states (:peeking-file router-data))))))
