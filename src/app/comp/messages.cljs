
(ns app.comp.messages
  (:require [clojure.string :as string]
            [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo-ui.colors :as colors]
            [respo.core :refer [defcomp list-> <> span div pre input button a]]
            [respo.comp.space :refer [=<]]
            [app.client-util :as util]
            [app.style :as style]
            ["dayjs" :as Dayjs]))

(defn on-clear [e d! m!] (d! :notify/clear nil))

(def style-message
  {:position :absolute,
   :right 8,
   :cursor :pointer,
   :font-weight 100,
   :font-family "Hind",
   :background-color (hsl 0 0 0 0.7),
   :border (str "1px solid " (hsl 0 0 100 0.2)),
   :padding "0 8px",
   :transition-duration "200ms"})

(defcomp
 comp-messages
 (messages)
 (list->
  :div
  {}
  (->> messages
       (take-last 2)
       (map-indexed
        (fn [idx msg]
          [(:id msg)
           (div
            {:style (merge
                     style-message
                     {:bottom (+ 12 (* idx 40))}
                     {:color (case (:kind msg)
                        :error (hsl 0 80 80)
                        :warning (hsl 60 80 80)
                        :info (hsl 240 80 80)
                        (hsl 120 80 80))}),
             :on {:click on-clear}}
            (<>
             (-> (:time msg) Dayjs (.format "mm:ss"))
             {:font-size 12, :font-family ui/font-code, :opacity 0.7})
            (=< 8 nil)
            (<> (:text msg) nil))])))))
