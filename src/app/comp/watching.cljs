
(ns app.comp.watching
  (:require [clojure.string :as string]
            [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo-ui.colors :as colors]
            [respo.core :refer [defcomp cursor-> <> span div input pre a]]
            [respo.comp.space :refer [=<]]
            [keycode.core :as keycode]
            [app.client-util :as util]
            [app.style :as style]
            [app.comp.expr :refer [comp-expr]]
            [app.theme :refer [base-style-leaf base-style-expr]]
            [app.util.dom :refer [inject-style]]
            [app.comp.theme-menu :refer [comp-theme-menu]]))

(def style-container {:padding "0 16px"})

(def style-tip
  {:font-family "Josefin Sans",
   :background-color (hsl 0 0 100 0.3),
   :border-radius "4px",
   :padding "4px 8px"})

(def style-title {:font-family "Josefin Sans"})

(defcomp
 comp-watching
 (states router-data theme)
 (let [expr (:expr router-data)
       focus (:focus router-data)
       bookmark (:bookmark router-data)
       others {}
       member-name (get-in router-data [:member :nickname])
       readonly? true]
   (if (nil? router-data)
     (div {:style style-container} (<> span "Session is missing!" nil))
     (if (:self? router-data)
       (div {:style style-container} (<> span "Watching at yourself :)" style-title))
       (div
        {:style (merge ui/column style-container)}
        (when (:working? router-data)
          (div
           {:style (merge ui/flex {:overflow :auto})}
           (inject-style ".cirru-expr" (base-style-expr (or theme :star-trail)))
           (inject-style ".cirru-leaf" (base-style-leaf (or theme :star-trail)))
           (cursor->
            (:id expr)
            comp-expr
            states
            expr
            focus
            []
            others
            false
            false
            readonly?
            (or theme :star-trail)
            0)))
        (=< nil 16)
        (div
         {}
         (<> span "Watching mode" style-tip)
         (=< 16 nil)
         (<> span member-name nil)
         (=< 16 nil)
         (<> span (:kind bookmark) nil)
         (=< 16 nil)
         (<> span (str (:ns bookmark) "/" (:extra bookmark)) nil)
         (=< 16 nil)
         (cursor-> :theme comp-theme-menu states (or theme :star-trail))))))))
