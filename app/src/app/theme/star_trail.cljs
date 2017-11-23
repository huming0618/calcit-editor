
(ns app.theme.star-trail
  (:require [hsl.core :refer [hsl]]
            [clojure.string :as string]
            [respo-ui.style :as ui]
            [polyfill.core :refer [text-width*]]
            [app.util :refer [simple?]]))

(def style-expr-beginner {:outline (str "1px solid " (hsl 200 80 70 0.2))})

(def style-expr-simple
  {:display :inline-block,
   :border-width "0 0 1px 0",
   :min-width 32,
   :padding-left 11,
   :padding-right 11,
   :vertical-align :top})

(def style-first {:color (hsl 40 85 60)})

(def style-space {:background-color (hsl 0 0 100 0.12)})

(def style-number {:color (hsl 0 70 40)})

(def style-highlight {:background-color (hsl 0 0 100 0.2)})

(def style-big {:border-right (str "16px solid " (hsl 0 0 30))})

(def style-keyword {:color (hsl 240 30 64)})

(def style-leaf
  (merge
   ui/input
   {:line-height "24px",
    :height 24,
    :margin "2px 2px",
    :padding "0px 4px",
    :background-color :transparent,
    :min-width 8,
    :color (hsl 200 14 60),
    :font-family "Menlo,Iosevka,Consolas,monospace",
    :font-size 15,
    :vertical-align :baseline,
    :transition-duration "200ms",
    :transition-property "color",
    :text-align :left,
    :border-width "1px 1px 1px 1px",
    :resize :none,
    :white-space :nowrap}))

(def style-string {:color (hsl 120 60 56)})

(def style-partial {:border-right (str "8px solid " (hsl 0 0 30)), :padding-right 0})

(defn decide-leaf-style [text focused? first? by-other?]
  (let [has-blank? (or (= text "") (string/includes? text " "))
        best-width (+
                    10
                    (text-width* text (:font-size style-leaf) (:font-family style-leaf)))
        max-width 240]
    (merge
     {:width (min best-width max-width)}
     (if first? style-first)
     (if (string/starts-with? text ":") style-keyword)
     (if (string/starts-with? text "|") style-string)
     (if (> best-width max-width) style-partial)
     (if (string/includes? text "\n") style-big)
     (if (re-find (re-pattern "^-?\\d") text) style-number)
     (if has-blank? style-space)
     (if (or focused? by-other?) style-highlight))))

(def style-expr-tail {:display :inline-block, :vertical-align :top, :padding-left 10})

(defn decide-expr-style [expr has-others? focused? tail? after-expr? beginner? length depth]
  (merge
   {}
   (if has-others? {:border-color (hsl 0 0 100 0.6)})
   (if focused? {:border-color (hsl 0 0 100 0.9)})
   (if (and (simple? expr) (not tail?) (not after-expr?) (pos? length)) style-expr-simple)
   (if tail? style-expr-tail)
   (if beginner? style-expr-beginner)))

(def style-expr
  {:border-width "0 0 0px 1px",
   :border-style :solid,
   :border-color (hsl 0 0 100 0.3),
   :min-height 24,
   :outline :none,
   :padding-left 10,
   :font-family "Menlo,monospace",
   :font-size 14,
   :margin-bottom 4,
   :margin-right 2,
   :margin-left 12,
   :margin-top 0})

(defn base-style-expr [] style-expr)

(defn base-style-leaf [] style-leaf)
