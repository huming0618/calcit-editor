
(ns app.comp.expr
  (:require [clojure.string :as string]
            [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo-ui.colors :as colors]
            [respo.core :refer [defcomp list-> cursor-> <> span div a]]
            [respo.comp.space :refer [=<]]
            [keycode.core :as keycode]
            [app.comp.leaf :refer [comp-leaf]]
            [app.client-util :refer [coord-contains? simple? leaf? expr?]]
            [app.util.shortcuts :refer [on-window-keydown on-paste!]]
            [app.theme :refer [decide-expr-theme]]
            [app.util :refer [tree->cirru]]
            [app.util.dom :refer [do-copy-logics!]]))

(defn on-focus [coord] (fn [e d! m!] (d! :writer/focus coord)))

(defn on-keydown [coord expr]
  (fn [e d! m!]
    (let [event (:original-event e)
          shift? (.-shiftKey event)
          meta? (or (.-metaKey event) (.-ctrlKey event))
          code (:key-code e)]
      (cond
        (and meta? (= code keycode/return)) (d! :ir/prepend-leaf nil)
        (= code keycode/return)
          (if (empty? coord)
            (d! :ir/prepend-leaf nil)
            (d! (if shift? :ir/expr-before :ir/expr-after) nil))
        (= code keycode/backspace) (d! :ir/delete-node nil)
        (= code keycode/space)
          (do (d! (if shift? :ir/leaf-before :ir/leaf-after) nil) (.preventDefault event))
        (= code keycode/tab)
          (do (d! (if shift? :ir/unindent :ir/indent) nil) (.preventDefault event))
        (= code keycode/up)
          (do (if (not (empty? coord)) (d! :writer/go-up nil)) (.preventDefault event))
        (= code keycode/down) (do (d! :writer/go-down nil) (.preventDefault event))
        (= code keycode/left) (do (d! :writer/go-left nil) (.preventDefault event))
        (= code keycode/right) (do (d! :writer/go-right nil) (.preventDefault event))
        (and meta? (= code keycode/c))
          (do-copy-logics! d! (pr-str (tree->cirru expr)) "Copied!")
        (and meta? (= code keycode/x))
          (do
           (do-copy-logics! d! (pr-str (tree->cirru expr)) "Copied!")
           (d! :ir/delete-node nil))
        (and meta? (= code keycode/v)) (on-paste! d!)
        (and meta? (= code keycode/b)) (d! :ir/duplicate nil)
        (and meta? (= code keycode/d))
          (do
           (d! :manual-state/abstract nil)
           (.preventDefault event)
           (js/setTimeout
            (fn []
              (let [el (.querySelector js/document ".el-abstract")]
                (if (some? el) (.focus el))))))
        :else
          (do
           (comment println "Keydown" (:key-code e))
           (on-window-keydown event d! {:name :editor}))))))

(defcomp
 comp-expr
 (states expr focus coord others tail? after-expr? readonly? theme depth)
 (let [focused? (= focus coord)
       first-id (apply min (keys (:data expr)))
       last-id (apply max (keys (:data expr)))
       sorted-children (->> (:data expr) (sort-by first))
       default-info {:after-expr? false}]
   (list->
    :div
    {:tab-index 0,
     :class-name (str "cirru-expr" (if focused? " cirru-focused" "")),
     :style (decide-expr-theme
             expr
             (contains? others coord)
             focused?
             tail?
             after-expr?
             (count coord)
             depth
             theme),
     :on (if readonly? {} {:keydown (on-keydown coord expr), :click (on-focus coord)})}
    (loop [result [], children sorted-children, info default-info]
      (if (empty? children)
        result
        (let [[k child] (first children)
              child-coord (conj coord k)
              partial-others (->> others
                                  (filter (fn [x] (coord-contains? x child-coord)))
                                  (into #{}))
              cursor-key (:id child)]
          (if (nil? cursor-key) (.warn js/console "[Editor] missing :id" (clj->js child)))
          (recur
           (conj
            result
            [k
             (if (= :leaf (:type child))
               (cursor->
                cursor-key
                comp-leaf
                states
                child
                focus
                child-coord
                (contains? partial-others child-coord)
                (= first-id k)
                readonly?
                theme)
               (cursor->
                cursor-key
                comp-expr
                states
                child
                focus
                child-coord
                partial-others
                (= last-id k)
                (:after-expr? info)
                readonly?
                theme
                (inc depth)))])
           (rest children)
           (assoc info :after-expr? (expr? child)))))))))
